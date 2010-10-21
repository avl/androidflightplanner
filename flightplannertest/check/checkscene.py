import sys
import json
import fplan.lib.mapper as mapper
from pyshapemerge2d import Line2,Polygon,vvector
import pyshapemerge2d



class Vertex(object):
    def __init__(self,subtree):
        self.nr=subtree['nr']
        self.used=subtree['used']=='true'
        self.what=subtree['what']
        self.merc=tuple(int(x) for x in [subtree['posx'],subtree['posy']])
        self.lastElev=subtree['lastElev']
    def __repr__(self):
        return "Vertex(%s,nr=#%d,used=%s,what=%s)"%(self.merc,self.nr,self.used,self.what)
    
class Triangle(object):
    def __init__(self,subtree):
        self.nr=subtree['nr']
        self.vertices=subtree['vertices']
    def get_lines(self):
        l=len(self.vertices)
        for i in xrange(l):
            a=self.vertices[i]
            b=self.vertices[(i+1)%l]
            yield (a,b)

def parse_pos(x):
    posx,posy,zoomlevel=x.split(",")
    return (int(posx),int(posy)),int(zoomlevel)
class Thing(object):
    def self_or_parent_has_triangles(self):
        if len(self.triangles)>0:
            return True
        if self.parentobj:
            return self.parentobj.self_or_parent_has_triangles()
        return False
    def __repr__(self):
        return "Thing(%s,%s)"%(self.merc,self.zoomlevel)
    def __init__(self,subtree):
        self.subtree=subtree
        self.merc,self.zoomlevel=parse_pos(subtree['pos'])
        self.pz=(self.merc,self.zoomlevel)
        self.triangles=subtree['triangles']

        self.size=(64<<13)>>self.zoomlevel
        if subtree['parent']==None:
            self.parent_merc=self.parent_zoom=None
            self.parentpz=None
        else:
            self.parent_merc,self.parent_zoom=parse_pos(subtree['parent'])
            self.parentpz=(self.parent_merc,self.parent_zoom)
        self.children=[parse_pos(x) for x in subtree['children']]
        self.edges=subtree['edges']
        self.base_vertices=subtree['base_vertices']
        self.center_vertex=subtree['center_vertex']
    def isinside(self,pos):
        x,y=pos
        return x>=self.merc[0] and x<=(self.merc[0]+self.size) and y>=self.merc[1] and y<=(self.merc[1]+self.size)
    def check_area(self,nr2tri):
        if len(self.triangles)==0:
            return
        totarea=0.0
        for trinr in self.triangles:
            tri=nr2tri[trinr]
            coords=[]
            for v in tri.vobjs:
                print "Pos: %s"%((int(v.merc[0]),int(v.merc[1])),)
                coords.append(pyshapemerge2d.Vertex(int(v.merc[0]),int(v.merc[1])))
            poly=Polygon(vvector(coords))
            assert not poly.is_ccw()
            area=-poly.calc_area()
            print "Tri:%s area = %s"%([x.nr for x in tri.vobjs],area)
            totarea+=area
        print "Area should be: %s (since size = %d)"%(self.size*self.size,self.size)
        print "Area is: %s"%(totarea)
        assert abs(totarea-(self.size*self.size))<10.0*10.0
    def check_parent_tris(self):
        if len(self.triangles)>0 and self.parentobj:
            assert not self.parentobj.self_or_parent_has_triangles()
            

class Scene(object):

    def prepare_lookups(self):
        self.poszoom2thing=dict()
        for thing in self.things:
            self.poszoom2thing[(thing.merc,thing.zoomlevel)]=thing
        self.vert2pos=dict()
        self.vnr2vobj=dict()
        for v in self.vertices:
            assert len(v.merc)==2
            self.vert2pos[v.nr]=v.merc
            self.vnr2vobj[v.nr]=v
        self.nr2tri=dict()
        for tri in self.triangles:
            self.nr2tri[tri.nr]=tri
            tri.vobjs=[]
            for vnr in tri.vertices:
                tri.vobjs.append(self.vnr2vobj[vnr])
        for thing in self.things:
            if thing.parentpz!=None:
                thing.parentobj=self.poszoom2thing[thing.parentpz]
            else:
                thing.parentobj=None
        
    def get_thing_by_poszoom(self,pz):
        assert len(pz)==2
        return self.poszoom2thing[pz]
        
    def __init__(self,dumpfile):
        self.tree=json.load(open(dumpfile))
        self.vertices=[]
        self.triangles=[]
        self.things=[]
        for subtree in self.tree['vertices']:
            self.vertices.append(Vertex(subtree))
        for subtree in self.tree['triangles']:
            self.triangles.append(Triangle(subtree))
        for subtree in self.tree['things']:
            self.things.append(Thing(subtree))

        self.prepare_lookups()    
    
    def find_cracks(self):
        lines=[]
        vertices=dict()
        for tri in self.triangles:
            for line in tri.get_lines():
                linev=(self.vnr2vobj[line[0]],self.vnr2vobj[line[1]])
                lines.append((linev,tri))
                vertices[linev[0].merc]=linev[0]
                vertices[linev[1].merc]=linev[1]
        for line,tri in lines:
            linemercs=[line[0].merc,line[1].merc]
            l=pyshapemerge2d.Line2(
                pyshapemerge2d.Vertex(int(line[0].merc[0]),int(line[0].merc[1])),
                pyshapemerge2d.Vertex(int(line[1].merc[0]),int(line[1].merc[1])))
                
            for vertmerc,vert in vertices.items():
                assert vertmerc==vert.merc
                if vert.merc in linemercs: continue
                v=pyshapemerge2d.Vertex(*vert.merc)
                dist=l.approx_dist(v)
                if dist<1:
                    print "Tri:",tri.nr
                    raise Exception("Likely crack detected - line %s (%s) is way too close to vertex %s (dist = %d)"%(l,line,vert,dist))

    def checkall(self):
        self.check_parent_child_consistency()
        self.check_vertices_in_things()
        self.check_areas()
        self.check_subsuming()
        self.find_cracks()
    def check_areas(self):
        for t in self.things:
            t.check_area(self.nr2tri)
    def check_subsuming(self):
        for t in self.things:
            t.check_parent_tris()
    
    def check_vertices_in_things(self):
        for thing in self.things:
            for triangle in thing.triangles:
                triangleObj=self.nr2tri[triangle]
                for idx in triangleObj.vertices:
                    vobj=self.vnr2vobj[idx] #self.vert2pos[idx]
                    if not vobj.used:
                        print "Thing %s, triangle %s, uses unused vertex: #%d:%s"%(thing,triangle,vobj.nr,vobj)
                        assert False
                    if not thing.isinside(vobj.merc):
                        print "Pos: %s is not inside <%s>"%(vobj,thing)
                        assert False
                    
    def check_parent_child_consistency(self):
        for thing in self.things:
            if thing.parentpz!=None:
                parentthing=self.get_thing_by_poszoom(thing.parentpz)
                if not (thing.pz in parentthing.children):
                    print "Thing %s has parent %s, but that parent doesn't have the thing."%(thing,parentthing)
                assert thing.pz in parentthing.children
            for childpz in thing.children:
                childobj=self.get_thing_by_poszoom(childpz)
                ourself=self.get_thing_by_poszoom(childobj.parentpz)
                if id(thing)!=id(ourself):
                    print "Child %s of Thing %s doesn't have correct parent"%(childpz,thing.pz)
                    print "The actual parent: %s"%(ourself.subtree,)
                    print "Should be: %s"%(thing.subtree,)
                assert id(thing)==id(ourself)
                
    
    
    
    

    
    
    
if __name__=='__main__':
    for arg in sys.argv[1:]:
        print "Analyzing dump:",arg
        analyzer=Scene(arg)
        try:
            analyzer.checkall()
        except:
            print "Error is in ",arg
            raise
            
            
