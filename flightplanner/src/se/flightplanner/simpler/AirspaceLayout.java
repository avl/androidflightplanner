package se.flightplanner.simpler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;


import se.flightplanner.AirspaceArea;
import se.flightplanner.simpler.AirspaceLayout.Rows;
import se.flightplanner.simpler.Common.Compartment;
import se.flightplanner.simpler.Common.Rect;
import se.flightplanner.simpler.FindNearby.FoundAirspace;
import se.flightplanner.vector.Pie;
import se.flightplanner.vector.Vector;

public class AirspaceLayout {	
	private int xsize,ysize;
	static public interface Measurer
	{
		Rect measure(AirspaceArea area);
	}
	Measurer measurer;
	
	public static class Cell
	{
		public Rect rect;
		public FoundAirspace area;
		public double pixelspacebefore;
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
		public Pie rowViewPie;
		public Row(Pie viewPie)
		{
			this.rowViewPie=viewPie;
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
		public void distribute_space()
		{
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
		}
		
		public boolean add_if_fits(FoundAirspace fa) {		
			
			if (!open) return false;
			
			Rect size=measurer.measure(fa.area);
			if (size.width()>xsize-used_width)
			{
				open=false;
				return false; //doesn'ẗ fit
			}
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
					cells.add(i,newcell);
					return true;
				}
				curx=cell.rect.right;
			}
			newcell.rect.offset(curx, 0);
			cells.add(newcell);
			return true;
		}
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
	}
	public class Rows
	{
		Pie comppie; //compartment pie
		public ArrayList<Row> rows=new ArrayList<AirspaceLayout.Row>();

		public Rows(Compartment comp) {
			comppie=Common.getPie(comp);

		}

		public Row getOpenRow(Compartment comp) {
			for(Row row:rows)
			{
				if (row.open)
					return row;
			}
			Row r=new Row(Common.getPie(comp));
			rows.add(r);
			return r;
		}

		public Row addRow(FoundAirspace fa,Compartment comp) {
			Row r=new Row(Common.getPie(comp));
			rows.add(r);
			Cell cell=new Cell();
			cell.area=fa;
			cell.rect=measurer.measure(fa.area);
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
	public void update(int xsize,int ysize)
	{
		comps.clear();
		this.xsize=xsize;
		this.ysize=ysize;
		//HashMap<Common.Compartment,ArrayList<FoundAirspace> >
		for(Entry<Compartment,ArrayList<FoundAirspace>> comp:nearby.get_spaces().entrySet())
		{
			Rows rows=layout_compartment(comp.getKey(),comp.getValue());
			
			comps.put(comp.getKey(), rows);
		}
		
		
		
		
	}
	private Rows layout_compartment(Compartment comp,ArrayList<FoundAirspace> foundspaces) {
		Rows rows=new Rows(comp);
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
			row.expand_big_spaces();
		}
		return rows;
	}
	public Rows getRows(Compartment comp) {
		
		return comps.get(comp);
	}	
}
