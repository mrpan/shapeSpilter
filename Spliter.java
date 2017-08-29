package com.gstool;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

import utils.Gps;
import utils.PositionUtil;

/**
 * Hello world!
 *
 */
public class Spliter 
{
	
	
	public void  start() throws Exception{
//		File file = JFileDataStoreChooser.showOpenFile("shp", null);
//        if (file == null) {
//            return;
//        }
		File file = new File("shp/test_line.shp");
		readShapefile(file);
        
	}
	
	@SuppressWarnings("rawtypes")
	public void readShapefile(File file) throws Exception{
		
	    Map<String, Object> map = new HashMap<String, Object>();
	    map.put("url", file.toURI().toURL());

	    DataStore dataStore = DataStoreFinder.getDataStore(map);
	    String typeName = dataStore.getTypeNames()[0];

	    FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
	            .getFeatureSource(typeName);
	    //Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

	    FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
	    FeatureIterator<SimpleFeature>  featureIterator = collection.features();
	    
	    readFeatures(featureIterator,file);
	}
	public void readFeatures(FeatureIterator featureIterator,File file) throws Exception{
		SimpleFeatureType featureType=null;
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		try{
	        while (featureIterator.hasNext()) {
	            SimpleFeature feature = (SimpleFeature) featureIterator.next();
	           
	            Long id = (Long) feature.getAttribute("id");
	            System.out.print(id);
	            System.out.print(": ");
//	            System.out.println(feature.	getDefaultGeometry());
	            Geometry geometry =  (Geometry) feature.getDefaultGeometry();
	            String geomType = geometry.getGeometryType();
	            System.out.println(geomType);
	            featureType = feature.getFeatureType();//feature类型
	            System.out.print(featureType);
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                featureBuilder.set("id",id);
	            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	            SimpleFeature multifeature=null;
	            switch(geomType){
	            case "MultiLineString":
	            	 int lineStringNum = geometry.getNumGeometries();
	            	 List<LineString> lineStrings = new ArrayList<LineString>(); 
                	 for(int i=0;i<lineStringNum;i++){//多个LineString
                		 LineString lineString = (LineString) geometry.getGeometryN(i);
//                		 lineStrings.add(lineString);
                		 lineStrings = createSegments(lineString,100);
//                		 lineStrings.addAll(lineSegments);
                		 for(int j=0;j<lineStrings.size();j++){
                    		 LineString line = lineStrings.get(j);
                    		 featureBuilder.add(line);
                    		 SimpleFeature lineFeature = featureBuilder.buildFeature( "fid" );
                    		 featureBuilder.set("id",id);
    		                 features.add(lineFeature);
                    	 }
                	 }    
	                 break;
	            case "LineString":
	            	LineString lineString =(LineString) geometry;
	            	List<LineString> lineSegments = createSegments(lineString,100);
	            	 for(int j=0;j<lineSegments.size();j++){
                		 LineString line = lineSegments.get(j);
                		 featureBuilder.add(line);
                		 SimpleFeature lineFeature = featureBuilder.buildFeature( "fid" );
                		 featureBuilder.set("id",id);
		                 features.add(lineFeature);
                	 }
	                 break;
	            }
	        }
	        outputSHP(file,features,featureType);
		}finally {
			featureIterator.close();
	    }
	}
	//thanks by Dalton Filho
	public List<LineString> createSegments(Geometry track, double segmentLength) throws NoSuchAuthorityCodeException, 
    FactoryException {

    GeodeticCalculator calculator = new GeodeticCalculator(CRS.decode("EPSG:4326")); // KML uses WGS84
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
    
    LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
    Collections.addAll(coordinates, track.getCoordinates());
    
    double accumulatedLength = 0;
    List<Coordinate> lastSegment = new ArrayList<Coordinate>();
    List<LineString> segments = new ArrayList<LineString>();
    Iterator<Coordinate> itCoordinates = coordinates.iterator();
    
    for (int i = 0; itCoordinates.hasNext() && i < coordinates.size() - 1; i++) {
        Coordinate c1 = coordinates.get(i);
        Coordinate c2 = coordinates.get(i + 1);
        
        lastSegment.add(c1);
        
        calculator.setStartingGeographicPoint(c1.x, c1.y);
        calculator.setDestinationGeographicPoint(c2.x, c2.y);
        
        double length = calculator.getOrthodromicDistance();
        
        if (length + accumulatedLength >= segmentLength) {
            double offsetLength = segmentLength - accumulatedLength;
            double ratio = offsetLength / length;
            double dx = c2.x - c1.x;
            double dy = c2.y - c1.y;
            
            Coordinate segmentationPoint = new Coordinate(c1.x + (dx * ratio), 
                                                          c1.y + (dy * ratio)); 
            
            lastSegment.add(segmentationPoint); // Last point of the segment is the segmentation point
            segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));
            
            lastSegment = new ArrayList<Coordinate>(); // Resets the variable since a new segment will be built
            accumulatedLength = 0D;
            coordinates.add(i + 1, segmentationPoint); 
        } else {
            accumulatedLength += length;
        }
    }
    
    lastSegment.add(coordinates.getLast()); // Because the last one is never added in the loop above
    segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));
    
    return segments;
}
	
	private void outputSHP(File file,List<SimpleFeature> features,SimpleFeatureType type) throws IOException{
		 File newFile = getNewShapeFile(file);

	     ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

	     Map<String, Serializable> params = new HashMap<String, Serializable>();
	     params.put("url", newFile.toURI().toURL());
	     params.put("create spatial index", Boolean.TRUE);

	     ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

	     /*
	      * TYPE is used as a template to describe the file contents
	      */
	     newDataStore.createSchema(type);
	     
	     /*
	      * Write the features to the shapefile
	      */
	     Transaction transaction = new DefaultTransaction("create");

	     String typeName = newDataStore.getTypeNames()[0];
	     SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
	     SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
	     System.out.println("SHAPE:"+SHAPE_TYPE);

	     if (featureSource instanceof SimpleFeatureStore) {
	         SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	         /*
	          * SimpleFeatureStore has a method to add features from a
	          * SimpleFeatureCollection object, so we use the ListFeatureCollection
	          * class to wrap our list of features.
	          */
	         SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
	         featureStore.setTransaction(transaction);
	         try {
	             featureStore.addFeatures(collection);
	             transaction.commit();
	         } catch (Exception problem) {
	             problem.printStackTrace();
	             transaction.rollback();
	         } finally {
	        	 newDataStore.dispose();
	             transaction.close();
	         }
	         JOptionPane.showMessageDialog(null,"分割完成", "PLAIN_MESSAGE", JOptionPane.PLAIN_MESSAGE);
	     } else {
	         System.out.println(typeName + " does not support read/write access");
	         System.exit(1);
	     }
	}
	     private static File getNewShapeFile(File file) {
		        String path = file.getAbsolutePath();
		        String newPath = path.substring(0, path.length() - 4) + ".shp";

		        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		        chooser.setDialogTitle("Save shapefile");
		        chooser.setSelectedFile(new File(newPath));

		        int returnVal = chooser.showSaveDialog(null);

		        if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
		            // the user cancelled the dialog
		            System.exit(0);
		        }

		        File newFile = chooser.getSelectedFile();
		        if (newFile.equals(file)) {
		            System.out.println("Error: cannot replace " + file);
		            System.exit(0);
		        }

		        return newFile;
		    }
    public static void main( String[] args ) throws Exception
    {
    	Spliter app = new Spliter();
    	app.start();
        System.out.println( "Hello World!" );
    }
}
