package se.flightplannertest.map3d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore;
import se.flightplanner.map3d.ElevationStore.Elev;

public class TestElevMap {

	@Test
	public void testElevMap() throws IOException
	{
		ElevationStore estore = getSampleEstore();
		Elev e=estore.get(new iMerc(1155450,635331), 13);
		Assert.assertTrue(e.loElev>=10 && e.hiElev<200);
		e=estore.get(new iMerc(1155450*10,635331), 13);
		Assert.assertTrue(e==null);
		e=estore.get(new iMerc(1155450,635331), 5);
		Assert.assertTrue(e.loElev==0); //ocean
		
		ByteArrayOutputStream bao=new ByteArrayOutputStream();
		DataOutputStream os=new DataOutputStream(new BufferedOutputStream(bao));
		estore.serialize(os);
		os.flush();
		BufferedInputStream bui=new BufferedInputStream(new ByteArrayInputStream(bao.toByteArray()));
		DataInputStream dai=new DataInputStream(bui);
		ElevationStore estore2=ElevationStore.deserialize(dai);
		e=estore2.get(new iMerc(1155450,635331), 13);
		Assert.assertTrue(e.loElev>=10 && e.hiElev<200);
		e=estore2.get(new iMerc(1155450*10,635331), 13);
		Assert.assertTrue(e==null);
		e=estore2.get(new iMerc(1155450,635331), 5);
		Assert.assertTrue(e.loElev==0); //ocean

		
	}

	static public ElevationStore getSampleEstore() throws FileNotFoundException,
			IOException {
		FileInputStream fi=new FileInputStream("/home/anders/saker/avl_traveltools/flightplanner/data/elev.bin");
		ElevationStore estore=ElevationStore.deserialize(new DataInputStream(new BufferedInputStream(fi)));
		return estore;
	}

}
