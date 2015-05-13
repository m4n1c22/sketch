/**
 * @file	SketchActivity.java
 * @author 	Sreeram Sadasivam
 * Application build for performing sketch operations in a distributed environment.
 * We make use of the middleware uMundo developed by TK group of TU Darmstadt for
 * performing this application.
 */
package org.umundo.samples.sketch;

import org.umundo.core.Discovery;
import org.umundo.core.Discovery.DiscoveryType;
import org.umundo.core.Message;
import org.umundo.core.Node;
import org.umundo.core.Publisher;
import org.umundo.core.Receiver;
import org.umundo.core.Subscriber;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SketchActivity extends Activity {


	Discovery disc;
	Node node;
	Publisher fooPub;
	Subscriber fooSub;

	RenderView rv;

	ViewDims vd;


	/** Class which stores a x and y coordinates of a point. */
	class Point {

		private float x;
		private float y;

		public void setX(float ix) {

			x= ix;
		}
		public void setY(float iy) {

			y= iy;
		}

		public float getX() {

			return x;
		}
		public float getY() {

			return y;
		}
		public void setPointWithPointObj(Point ip) {
			x = ip.getX();
			y = ip.getY();
		}
	}

	/** Class for Storing dimensions of a View.*/
	class ViewDims implements Serializable{

		private Point P;
		private float width;
		private float height;

		/*Constructor*/
		public ViewDims() {

			width 	= 0;
			height 	= 0;
			P = new Point();
		}
		/** Getter methods */
		public Point getPoint() {
			return P;
		}
		public float getWidth() {
			return width;
		}
		public float getHeight() {
			return height;
		}
		/** Setter Methods */
		public void setPoint(Point iP) {
			P.setPointWithPointObj(iP);
		}
		public void setWidth(float iw) {
			width = iw;
		}
		public void setHeight(float ih) {
			height = ih;
		}

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

	/** Custom View for drawing */
	class RenderView extends View
	{
		Paint pixelpaint;
		Point P;
		float scaleRatio;

		public float getX() {

			return P.getX();
		}
		public float getY() {

			return P.getY();
		}
		public Point getPoint() {

			return P;
		}

		public void setPoint(Point iP) {

			P.setPointWithPointObj(iP);
		}
		public void setXandY(float ix,float iy) {
			P.setX(ix);
			P.setY(iy);
		}
		/**
		 * Setting X and Y relative to the device. Here the mapping of the incoming coordinates are
		 * evaluated with its width and height and the scale ratio is determined and this value is
		 * mapped to the new set of coordinates.
		 */
		public void setXandYRelative(float ix,float iy,float h,float w) {
			float devHeight=this.getHeight();
			float devWidth=this.getWidth();

			scaleRatio = devHeight/h;
			setXandY(ix*devWidth/w,iy*devHeight/h);
		}

		public RenderView(Context context)
		{
			super(context);
			P = new Point();
			scaleRatio=1;
			pixelpaint = new Paint();
			pixelpaint.setStrokeCap(Paint.Cap.ROUND);
			pixelpaint.setStrokeWidth(0);
			pixelpaint.setColor(Color.RED);
			pixelpaint.setStyle(Paint.Style.FILL);
		}

		protected void onDraw(Canvas canvas)
		{
			//canvas.drawPoint(x, y, pixelpaint);
			canvas.drawCircle(P.getX(), P.getY(), 10*scaleRatio, pixelpaint);
			invalidate();
		}
	}

	public class TestReceiver extends Receiver {


		/**
		 * Method extracts x,y, height and width from the received message.
		 * */
		public void setViewWithIncomingViewDims() {

			rv.setXandYRelative(vd.getPoint().getX(),vd.getPoint().getY(),vd.getHeight(),vd.getWidth());
		}
		public void receive(Message msg) {

			//TODO: Add Code for deserialization of received message to ViewDimso object...
			setViewWithIncomingViewDims();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rv = new RenderView(this);
		vd = new ViewDims();


		setContentView(rv);

		rv.setXandY(200,200);

		rv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				Log.d("Touch X", String.valueOf(event.getX()));
				Log.d("Touch Y", String.valueOf(event.getY()));
				rv.setXandY(event.getX(), event.getY());
				vd.setPoint(rv.getPoint());
				vd.setHeight(rv.getHeight());
				vd.setWidth(rv.getWidth());
				/*TODO: Change the following code for send with an extra param of serialization of message.
				 */
				fooPub.send(vd.serialize()); //This line is compilation issue.
				return true;
			}
		});


		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifi != null) {
			MulticastLock mcLock = wifi.createMulticastLock("mylock");
			mcLock.acquire();
			// mcLock.release();
		} else {
			Log.v("android-umundo", "Cannot get WifiManager");
		}

//		System.loadLibrary("umundoNativeJava");
		System.loadLibrary("umundoNativeJava_d");

		disc = new Discovery(DiscoveryType.MDNS);
    
		node = new Node();
		disc.add(node);

		fooPub = new Publisher("sketch");
		node.addPublisher(fooPub);
    
		fooSub = new Subscriber("sketch", new TestReceiver());
		node.addSubscriber(fooSub);

	}
}
