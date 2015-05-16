/**
 * @file	SketchActivity.java
 * @author 	Sreeram Sadasivam
 * Application build for performing sketch operations in a distributed environment.
 * We make use of the middleware uMundo developed by TK group of TU Darmstadt for
 * performing this application.
 */
package com.umundo.sketch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.umundo.core.Discovery;
import org.umundo.core.Discovery.DiscoveryType;
import org.umundo.core.Message;
import org.umundo.core.Node;
import org.umundo.core.Publisher;
import org.umundo.core.Receiver;
import org.umundo.core.Subscriber;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


/** Class which stores x and y coordinates of a point. */
class Point implements Serializable{

	private float x;
	private float y;

	/**
	 * This method is used to set X coordinate of the point.
	 * @param ix	incoming x-coordinate which has to be set to the point object.
	 */
	public void setX(float ix) {

		x= ix;
	}
	/**
	 * This method is used to set Y coordinate of the point.
	 * @param iy	incoming y-coordinate which has to be set to the point object.
	 */
	public void setY(float iy) {

		y= iy;
	}
	/**
	 * This method is used to get X coordinate of the point.
	 * @return x-coordinate of the point object.
	 */
	public float getX() {

		return x;
	}
	/**
	 * This method is used to get Y coordinate of the point.
	 * @return y-coordinate of the point object.
	 */
	public float getY() {

		return y;
	}

	/**
	 * This method is used to set Given point with another object of point class.
	 * @param ip incoming point which is used to set the given point class object.
	 */
	public void setPointWithPointObj(Point ip) {
		x = ip.getX();
		y = ip.getY();
	}
}

/** Class for Storing dimensions of a View.*/
class ViewDims implements Serializable{

	/** Point object which stores the x and y coordinates of the touch view. */
	private Point P;
	/** Stores width of the parent view which the touch is part of. */
	private float width;
	/** Stores height of the parent view which the touch is part of. */
	private float height;

	/** Constructor*/
	public ViewDims() {

		width 	= 0;
		height 	= 0;
		P = new Point();
	}

	/** Getter methods */
	/**
	 * Get the point of Touch in the View.
	 * @return Touch point object.
	 */
	public Point getPoint() {
			return P;
	}

	/**
	 * Get the width of the View.
	 * @return View Width
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Get the height of the View.
	 * @return View height
	 */
	public float getHeight() {
		return height;
	}

	/** Setter Methods */
	/**
	 * Set the point of Touch in the View with the new incoming point.
	 * @param iP incoming point set as the new Touched point.
	 */
	public void setPoint(Point iP) {
			P.setPointWithPointObj(iP);
	}

	/**
	 * Set the View width.
	 * @param iw new View Width which is set to the Vie.
	 */
	public void setWidth(float iw) {
		width = iw;
	}

	/**
	 * Set the View height.
	 * @param ih new View Height which is set to the View.
	 */
	public void setHeight(float ih) {
		height = ih;
	}

	/**
	 * Serialize method for serializing the object of ViewDims class. This method creates a
	 * serialized byte array of the object of class ViewDims. This method is used before publishing
	 * the data in uMundo.
	 * @return serialized byte array which is used by the application for transforming this class
	 * object into serialized byte array.
	 * */
	public byte[] serialize() {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(this);
			return b.toByteArray();
		}
		catch(IOException ioe)
		{
			//Handle logging exception
			return null;
		}
	}
	/**
	 * De-Serialize method for de-serializing the byte array to the object of ViewDims class. This
	 * method creates a serialized byte array of the object of class ViewDims. This method is used
	 * after receiving the data from uMundo.
	 * @param bytes - byte array which is obtained from the receive class and is deserialized by
	 *              this method and transformed into an object.
	 * @return An object is returned after transforming a byte array into an object. This process is
	 * called as de-serialization.
	 * */
	public Object deserialize(byte[] bytes)  {
		try {
			ByteArrayInputStream b = new ByteArrayInputStream(bytes);
			ObjectInputStream o = new ObjectInputStream(b);
			return o.readObject();
		}
		catch(Exception e){
			//Handle logging exception
			return null;
		}
	}
}

/**
 * The main class which performs sketching application. It makes use of the uMundo Library for
 * performing the operations of pub-sub systems. This class acts like a controller in a
 * Model-View-Controller model.
 * */

public class SketchActivity extends Activity {

	/**
	 * Discovery object is used in this application to find the multicast DNS which can then be
	 * used to perform zero configuration systems.
	 */
	Discovery disc;
	/**
	 * A node is created when the application is launched and added in the discovery list of nodes.
	 * */
	Node node;
	/**
	 * Publisher object which is used in the application to send points on touch events to the
	 * subscriber system.
	 * */
	Publisher fooPub;
	/**
	 * Subscriber object which is used in the application to receive points from Publishing object/s
	 * on delivering the events.
	 * */
	Subscriber fooSub;

	/**
	 * Custom view used to perform drawing operation in the application.
	 * */
	RenderView rv;

	/**
	 * Stores the dimensions required for the touch view.
	 * */
	ViewDims vd;

	/**
	 * Standard Device Height used for scaling an object when drawing.
	 * */
	final int STANDARD_DEVICE_HEIGHT = 640;

	/**
	 *  Custom View for drawing the sketches based on touch points provided by the user.
	 *  */
	class RenderView extends View
	{
		/** Paint object used for drawing points with different graphics options. */
		Paint pixelpaint;
		/** Point object which records the current touch point in the view. */
		Point P;
		/**
		 * Scale ratio is used for scaling the coordinates when porting the coordinates from one
		 * device to another.
		 * */
		float scaleRatio;

		/**
		 * This arraylist stores the array of points which has been/has to be drawn in the view.
		 * */
		ArrayList<Point> arrayPoints;
		/**
		 * This method is used to get X coordinate of the point.
		 * @return x-coordinate of the point object.
		 */
		public float getX() {

			return P.getX();
		}
		/**
		 * This method is used to get Y coordinate of the point.
		 * @return y-coordinate of the point object.
		 */
		public float getY() {

			return P.getY();
		}

		/**
		 * This method is used to get the currently touched point.
		 * @return Current Touch point object.
		 */

		public Point getPoint() {

			return P;
		}

		/**
		 * This method is used to set the currently touched point in the view.
		 * @param iP incoming point set as the new Touched point.
		 */
		public void setPoint(Point iP) {

			P.setPointWithPointObj(iP);
		}
		/**
		 *	This method is used to set the X and Y coordinate of the touched point in the view.
		 *  @param ix X-Coordinate of the touched Point.
		 *  @param iy Y-Coordinate of the touched Point.
		 */
		public void setXandY(float ix,float iy) {
			P.setX(ix);
			P.setY(iy);
		}
		/**
		 * Setting X and Y relative to the device. Here the mapping of the incoming coordinates are
		 * evaluated with its width and height and the scale ratio is determined and this value is
		 * mapped to the new set of coordinates.
		 * @param ix X-Coordinate of the Received Point.
		 * @param iy Y-Coordinate of the Received Point.
		 * @param h  Sender's Device Height
		 * @param w  Sender's Device Width
		 */
		public void setXandYRelative(float ix,float iy,float h,float w) {
			float devHeight=this.getHeight();
			float devWidth=this.getWidth();

			scaleRatio = devHeight/h;
			setXandY(ix * devWidth / w, iy * devHeight / h);
		}

		/** Constructor of Renderview which */
		public RenderView(Context context)
		{
			super(context);
			P = new Point();
			int size = this.getHeight()*this.getWidth();
			arrayPoints= new ArrayList<Point>(size);
			scaleRatio=1;
			pixelpaint = new Paint();
			pixelpaint.setStrokeCap(Paint.Cap.ROUND);
			pixelpaint.setStrokeWidth(0);
			pixelpaint.setColor(Color.RED);
			pixelpaint.setStyle(Paint.Style.FILL);
		}

		/**
		 * onDraw method is called whenever the view is reloaded.
		 * @param canvas Canvas objec used for performing View Drawing based operations.
		 * */
		protected void onDraw(Canvas canvas)
		{
			/**
			 * Array of points are drawn using the method drawcircle provided by canvas object.
			 **/
			for(Point p:arrayPoints) {
				canvas.drawCircle(p.getX(), p.getY(), 10 * this.getHeight() / STANDARD_DEVICE_HEIGHT, pixelpaint);
			}
			invalidate();
		}
	}

	/**
	 * Receiver class which receives the messages which the node has subscribed to.
	 **/
	public class TestReceiver extends Receiver {


		/** Function which sets the view with received view dimensions. */
		public void setViewWithIncomingViewDims() {

			SketchActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {

					/**
					 * Setting X and Y relative to the device. Here the mapping of the incoming
					 * coordinates are evaluated with its width and height and the scale ratio is
					 * determined and this value is mapped to the new set of coordinates.
					 */

					float devHeight = rv.getHeight();
					float devWidth = rv.getWidth();

					float scaleRatioHeight, scaleRatioWidth;
					scaleRatioHeight = devHeight / vd.getHeight();
					scaleRatioWidth = devWidth / vd.getWidth();

					Point P = new Point();
					P.setX(vd.getPoint().getX() * scaleRatioWidth);
					P.setY(vd.getPoint().getY() * scaleRatioHeight);

					/** Adding the received touch point to the array of points.*/
					rv.arrayPoints.add(P);
					/** Reloading the view. */
					rv.invalidate();
				}
			});
		}
		/**
		 * Receive method is called when a subscriber receives a message for the subscribed
		 * channel.
		 * @param msg Received message by the subscriber.
		 * */
		public void receive(Message msg) {

			/** De-serializing the received message and transforming it into ViewDims object.*/
			vd = (ViewDims) vd.deserialize(msg.getData());
			/** Setting the view with the received view dimensions. */
			setViewWithIncomingViewDims();
		}
	}

	/** Called when the activity is first created.*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/** Created render view and view dimensions. */
		rv = new RenderView(this);
		vd = new ViewDims();


		/** Setting render view as the contentview of the activity.*/
		setContentView(rv);

		/** Setting an OnTouch Listener with the content view.*/
		rv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				/**
				 * Creating the point object and recording the touched points in the Point object.
				 **/
				Point P = new Point();
				P.setX(event.getX());
				P.setY(event.getY());
				/** Adding the touch event point into the array of points.*/
				rv.arrayPoints.add(P);
				/** Reloading the view. */
				rv.invalidate();
				/**
				 * Setting the view dimensions for sending a serializable object to the
				 * subscriber.
				 **/
				vd.setPoint(P);
				vd.setHeight(rv.getHeight());
				vd.setWidth(rv.getWidth());
				/**
				 * Publishing the touch event to the subscribers.
				 **/
				fooPub.send(vd.serialize());

				return true;
			}
		});


		/**
		 * Creating a wifi manager object for creating a multicast lock inorder to perform the
		 * mDNS operations used by uMundo Library.
		 * */
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifi != null) {
			MulticastLock mcLock = wifi.createMulticastLock("mylock");
			mcLock.acquire();
		} else {
			Log.v("android-umundo", "Cannot get WifiManager");
		}

		/**
		 * Loading the uMundoNative liibrary.
		 * */
		System.loadLibrary("umundoNativeJava_d");

		/** Allocating the discovery node and setting the type as multicast DNS */
		disc = new Discovery(DiscoveryType.MDNS);

		/** Creating a new node.*/
		node = new Node();
		/** Adding the created node into the discovery list.*/
		disc.add(node);

		/** Creating a publisher object with the channel name as sketch.*/
		fooPub = new Publisher("sketch");
		/** Adding the publisher to the node in the network which created few steps back.*/
		node.addPublisher(fooPub);

		/** Creating a subscriber object with the channel name as sketch.*/
		fooSub = new Subscriber("sketch", new TestReceiver());
		/** Adding the subscriber to the node in the network which created few steps back.*/
		node.addSubscriber(fooSub);
	}
}
