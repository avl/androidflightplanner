import sys
import json
import fplan.lib.mapper as mapper
from pyshapemerge2d import Line2,Polygon,vvector
import pyshapemerge2d



class Vertex(object):
    def __init__(self,subtree):
        self.nr=subtree['nr']
        self.used=subtree['used']=='true'
        self.merc=tuple(int(x) for x in [subtree['posx'],subtree['posy']])
        self.lastElev=subtree['lastElev']
class Triangle(object):
    def __init__(self,subtree):
        self.nr=subtree['nr']
        self.vertices=subtree['vertices']



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
    def checkall(self):
        self.check_parent_child_consistency()
        self.check_vertices_in_things()
        self.check_areas()
        self.check_subsuming()
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
                    pos=self.vert2pos[idx]
                    assert thing.isinside(pos)
                    
    def check_parent_child_consistency(self):
        for thing in self.things:
            if thing.parentpz!=None:
                parentthing=self.get_thing_by_poszoom(thing.parentpz)
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
    analyzer=Scene(sys.argv[1])
    analyzer.checkall()
        
