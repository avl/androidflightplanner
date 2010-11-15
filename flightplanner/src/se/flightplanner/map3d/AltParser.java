package se.flightplanner.map3d;

public class AltParser {

	public int parseAlt(String alt)
	{
		alt=alt.toLowerCase().trim();
		if (alt.equals("unl"))
			return 20000;
		if (alt.equals("gnd"))
			return 0;
		if (alt.endsWith("ft"))
		{
			try
			{
				return (int)(Float.parseFloat(alt.substring(0,alt.length()-2).trim()));				
			}
			catch(NumberFormatException e)
			{				
			}
		}
		if (alt.startsWith("fl"))
		{
			try
			{
				return (int)(100.0f*Float.parseFloat(alt.substring(2).trim()));				
			}
			catch(NumberFormatException e)
			{				
			}
		}
		String num="";
		boolean fl=alt.contains("fl");
		for(int i=0;i<alt.length();++i)		
		{
			char c=alt.charAt(i);
			if (Character.isDigit(c) || c=='.')
				num+=c;			
		}
		if (num.equals(""))
			return 0;
		float fnum;
		try
		{
			fnum=Float.parseFloat(num);			
		}
		catch(NumberFormatException e)
		{
			throw new RuntimeException("Unexpected error in AltParser");
		}
		if (fl)
			fnum*=100.0f;
		return (int)fnum;
	}
}
