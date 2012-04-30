package se.flightplanner.simpler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import android.util.Log;


import se.flightplanner.AirspaceArea;
import se.flightplanner.simpler.AirspaceLayout.Rows;
import se.flightplanner.simpler.Common.Compartment;
import se.flightplanner.simpler.Common.Rect;
import se.flightplanner.simpler.FindNearby.FoundAirspace;
import se.flightplanner.vector.Pie;
import se.flightplanner.vector.Vector;

public class AirspaceLayout {	
	
	static public interface Measurer
	{
		Rect measure(AirspaceArea area);
	}
	private Measurer measurer;
	
	public static class Cell
	{
		public Rect rect;
		public FoundAirspace area;
		public boolean locked;
		//public double pixelspacebefore;
		public String toString()
		{
			return "Cell("+area.area.name+", "+rect+")";
		}
	}
	public class Row
	{
		int used_width;
		public ArrayList<Cell> cells=new ArrayList<Cell>();		
		public boolean open=true;
		public double closest_on_row;
		public Pie rowViewPie;
		private int xsize;
		private void pushLeft(int idx,int ammount) {
			int remain=ammount;
			for(int i=idx;i>=0;--i)
			{
				
				int curslack;
				if (i==0)
				{
					curslack=cells.get(i).rect.left;
				}
				else
				{
					curslack=cells.get(i).rect.left - cells.get(i-1).rect.right;					
				}
				cells.get(i).rect.offset(-remain, 0);
				remain-=curslack;
				if (remain<=0)
					break;								
			}						
		}
		private void pushRight(int idx,int ammount) {
			int remain=ammount;
			for(int i=idx;i<cells.size();++i)
			{
				
				int curslack;
				if (i==cells.size()-1)
				{
					curslack=xsize-cells.get(i).rect.right;
				}
				else
				{
					curslack=cells.get(i+1).rect.left - cells.get(i).rect.right;					
				}
				cells.get(i).rect.offset(remain, 0);
				remain-=curslack;
				if (remain<=0)
					break;								
			}						
		}
		private int findLeftSlack(int idx) {
			int slack=0;
			for(int i=idx;i>=0;--i)
			{
				if (cells.get(i).locked) break;
				if (i==0)
				{
					slack+=cells.get(i).rect.left;
				}
				else
				{
					slack+=cells.get(i).rect.left - cells.get(i-1).rect.right;					
				}
			}
			return slack;
		}
		private int findRightSlack(int idx) {
			int slack=0;
			for(int i=idx;i<cells.size();++i)
			{
				if (i==cells.size()-1)
				{
					slack+=xsize-cells.get(i).rect.right;
				}
				else
				{
					slack+=cells.get(i+1).rect.left - cells.get(i).rect.right;					
				}
			}
			return slack;
		}
		
		public Row(Pie viewPie,int xsize)
		{
			if (viewPie==null)
				throw new RuntimeException("Internal error");
			this.rowViewPie=viewPie;
			this.xsize=xsize;
		}
		public String toString()
		{
			StringBuilder sb=new StringBuilder();
			sb.append("Row(");
			for(Cell cell:cells)
				sb.append(" "+cell.toString());
			sb.append(" )");
			
			return sb.toString();
		}
		int nominal_position(Cell cell)
		{
			//Log.i("fplan","Relpos Cell:"+cell+"area:"+cell.area);
			//Log.i("fplan","rwp:"+rowViewPie+"pie:"+cell.area.pie);
			int nominal_center=(int)(xsize*(rowViewPie.getRelativePos(cell.area.relbearing)));
			return nominal_center;
		}
		int findCellIdx(Cell cell)
		{
			for(int i=0;i<cells.size();++i)
				if (cells.get(i)==cell)
					return i;
			return -1;
		}
		public void distribute_space()
		{
			
			ArrayList<Cell> dist_order=new ArrayList<Cell>();
			for(Cell cell:cells)
				dist_order.add(cell);
			Collections.sort(dist_order,new Comparator<Cell>(){
				@Override
				public int compare(Cell lhs, Cell rhs) {
					return Double.compare(lhs.area.distance, rhs.area.distance);
				}
			});
			
			for(Cell cell:dist_order)
			{ //Smallest distance first
				int idx=findCellIdx(cell);
				int nompos=nominal_position(cell);
				int delta=nompos-cell.rect.left;
				if (delta<0)
				{
					int moveleft=-delta;
					int slack=findLeftSlack(idx);
					if (moveleft>slack) moveleft=slack;
					pushLeft(idx,moveleft);					
				}
				else					
				{
					int moveright=delta;
					int slack=findRightSlack(idx);
					if (moveright>slack) moveright=slack;
					pushRight(idx,moveright);					
				}
				cell.locked=true;
			}	
			
			/*
			int used=0;
			ArrayList<Cell> angles=new ArrayList<Cell>();
			for(Cell cell:cells) 
			{
				used+=cell.rect.width();
				angles.add(cell);
			}
			Collections.sort(angles,new Comparator<Cell>()
					{
						@Override
						public int compare(Cell lhs, Cell rhs) {
							return Double.compare(lhs.area.pie.getA(), rhs.area.pie.getA());
						}
					});			
			int free_pixels=xsize-used;
			double free_angle=0;
			{
				double cur_angle=rowViewPie.getA();
				for(Cell cell:angles)
				{
					Pie pie=cell.area.pie;
					double a=pie.getA();
					double b=pie.getB();
					if (a>cur_angle)
					{
						free_angle+=a-cur_angle;
					}
					if (b>cur_angle)
						cur_angle=b;
				}
				if (cur_angle<rowViewPie.getB())
					free_angle+=rowViewPie.getB()-cur_angle;
			}
			if (free_angle>1e-6)
			{
				double pixels_per_angle=free_pixels/free_angle;
				double cur_angle=rowViewPie.getA();
				for(Cell cell:angles)
				{
					Pie pie=cell.area.pie;
					if (pie.getA()>cur_angle)
					{
						cell.pixelspacebefore+=(pie.getA()-cur_angle)*pixels_per_angle;						
					}					
					if (pie.getB()>cur_angle)
						cur_angle=pie.getB();
				}
				
				
			}
			
			double accum_move=0;
			for(Cell cell:cells)
			{
				accum_move+=cell.pixelspacebefore;
				cell.rect.offset((int)accum_move,0);
			}
			*/
		}
		
		public boolean add_if_fits(FoundAirspace fa) {		
			
			if (cells.size()==0)
			{
				Cell newcell=new Cell();
				newcell.area=fa;
				newcell.rect=measurer.measure(fa.area);
				if (newcell.rect.width()>xsize)
				{
					newcell.rect.right-=newcell.rect.width()-xsize;
				}
				used_width+=newcell.rect.width();
				closest_on_row=fa.distance;
				
				cells.add(newcell);
				return true;
			}
			
			if (!open) return false;
			Rect size=measurer.measure(fa.area);
			//Log.i("fplan.al","Checking if "+fa.area.name+" fits. Used:"+used_width+" xsize: "+xsize+" cursize: "+size.width());
			
			//Log.i("fplan","Aspace:"+fa.area.name+" distance: "+fa.distance+" closest on this row: "+closest_on_row);
			if (fa.distance>250 && cells.size()>0 && fa.distance>2*closest_on_row)
			{ //too far away from others on same row.
				open=false;
				return false;
			}
				
			if (size.width()>xsize-used_width)
			{
				open=false;
				//Log.i("fplan.al"," - It doesn't");
				return false; //doesn'ẗ fit
			}
			//Log.i("fplan.al"," - It does.");
			Cell newcell=new Cell();
			newcell.area=fa;
			int curx=0;
			newcell.rect=size;
			for(int i=0;i<cells.size();++i)
			{
				Cell cell=cells.get(i);
				if (cell.area.pie.isAtAllRightOf(fa.pie))
				{
					newcell.rect.offset(curx, 0);
					int cw=newcell.rect.width();
					used_width+=cw;
					cells.add(i,newcell);
					i+=1;
					for(;i<cells.size();++i)
					{
						cells.get(i).rect.offset(cw,0);
					}
					return true;
				}
				curx=cell.rect.right;
			}
			newcell.rect.offset(curx, 0);
			used_width+=newcell.rect.width();
			cells.add(newcell);
			return true;
		}
		/*
		public void expand_big_spaces() {
			int[] wantgrow=new int[cells.size()];

			for(int i=0;i<cells.size();++i)
			{
				Cell cell=cells.get(i);
				int want_width=(int)(0.5*xsize*(cell.area.pie.getSize()/rowViewPie.getSize()));
				int is_width=cell.rect.width()/2;
				if (want_width>is_width)
					wantgrow[i]=want_width-is_width;
			}
			for(int i=0;i<cells.size();++i)
			{				
				Cell cell=cells.get(i);
				if (i==0)
				{
					int grow=wantgrow[i];
					cell.rect.left-=grow;
					if (cell.rect.left<0)
						cell.rect.left=0;
				}
				if (i==cells.size()-1)
				{
					int grow=wantgrow[i];					
					cell.rect.right+=grow;
					if (cell.rect.right>xsize)
						cell.rect.right=xsize;
					continue;
				}
				if (i==0) continue;
				Cell a=cells.get(i);
				Cell b=cells.get(i+1);
				int free=(int)b.pixelspacebefore;
				int a_grow=wantgrow[i];
				int b_grow=wantgrow[i+1];
				if (a_grow+b_grow<free)
				{//no contention
					a.rect.right+=a_grow;
					b.rect.left-=b_grow;
					continue;					
				}
				//contention
				
				int contended=(a_grow+b_grow)-free;
				//free = a_grow+b_grow-contended
				//free = a_grow-contended+b_grow-contended +contended
				//free = a_grow-contended+b_grow-contended +p*contended+(1-p)*contended
				if (contended>free)
					contended=free;
				int uncontended=free-contended;
				double a_prefer_factor=a.area.pie.getSize()/(a.area.pie.getSize()+b.area.pie.getSize());
				double f=a_prefer_factor;
				int asize=(int)(a_grow-contended+f*contended);			
				int bsize=free-asize;
				a.rect.right+=asize;
				b.rect.left-=bsize;
			}		
			
			
		}
		*/
	}
	public class Rows
	{
		private int xsize;
		Pie comppie; //compartment pie
		public ArrayList<Row> rows=new ArrayList<AirspaceLayout.Row>();

		public Rows(Compartment comp,int xsize) {
			comppie=Common.getPie(comp);
			this.xsize=xsize;

		}

		public Row getOpenRow(Compartment comp) {
			if (rows.size()>0)
			{
				Row last=rows.get(rows.size()-1);
				if (last.open)
				{
					return last;
				}
			}
			Row r=new Row(Common.getPie(comp),xsize);
			rows.add(r);
			return r;
		}

		public Row addRow(FoundAirspace fa,Compartment comp) {
			Row r=new Row(Common.getPie(comp),xsize);
			rows.add(r);
			Cell cell=new Cell();
			cell.area=fa;			
			cell.rect=measurer.measure(fa.area);
			int cwid=cell.rect.width();
			if (cwid>xsize)
			{				
				int delta=cwid-xsize;
				cell.rect.right-=delta;
			}
			r.used_width+=cell.rect.width();
			r.closest_on_row=fa.distance;
			
			r.cells.add(cell);
			return r;
		}
	}
	private HashMap<Compartment,Rows> comps=new HashMap<Common.Compartment, AirspaceLayout.Rows>();
	private FindNearby nearby;
	public AirspaceLayout(Measurer meas,FindNearby nearby)
	{
		this.measurer=meas;
		assert nearby!=null;
		this.nearby=nearby;
	}	
	public void update(int xsize_left,int xsize_ahead,int xsize_right,int xsize_present)
	{
		comps.clear();
		HashMap<Compartment,Integer> comp2size=new HashMap<Compartment,Integer>();
		comp2size.put(Compartment.AHEAD, xsize_ahead);
		comp2size.put(Compartment.LEFT, xsize_left);
		comp2size.put(Compartment.RIGHT, xsize_right);
		comp2size.put(Compartment.PRESENT, xsize_present);		
		//HashMap<Common.Compartment,ArrayList<FoundAirspace> >
		for(Entry<Compartment,ArrayList<FoundAirspace>> comp:nearby.get_spaces().entrySet())
		{			
			Rows rows=layout_compartment(comp.getKey(),comp.getValue(),comp2size.get(comp.getKey()));
			
			comps.put(comp.getKey(), rows);
		}
		
		
		
		
	}
	private Rows layout_compartment(Compartment comp,ArrayList<FoundAirspace> foundspaces, int xsize) {
		Rows rows=new Rows(comp,xsize);
		for(FoundAirspace fa:foundspaces)
		{
			Row row=rows.getOpenRow(comp);
			if (!row.add_if_fits(fa))
			{
				rows.addRow(fa,comp);								
			}
					
		}
		
		for(Row row:rows.rows)
		{
			row.distribute_space();
			//row.expand_big_spaces();
		}
		return rows;
	}
	public Rows getRows(Compartment comp) {
		
		return comps.get(comp);
	}	
}
