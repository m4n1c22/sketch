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

import java.io.UnsupportedEncodingException;

public class SketchActivity extends Activity {


	Discovery disc;
	Node node;
	Publisher fooPub;
	Subscriber fooSub;

	RenderView rv;

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
	}


	/** Custom View for drawing */
	class RenderView extends View
	{
		Paint pixelpaint;
		Point P;

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

			P.setX(iP.getX());
			P.setY(iP.getY());
		}
		public void setXandY(float ix,float iy) {
			P.setX(ix);
			P.setY(iy);
		}
		/**
		 * Setting X and Y relative to the device. Here the mapping of the incoming coordinates are
		 * evaluated with its width and height and the aspect ratio is determined and this value is
		 * mapped to the new set of coordinates.
		 */
		public void setXandYRelative(float ix,float iy,float h,float w) {
			float devHeight=this.getHeight();
			float devWidth=this.getWidth();

			setXandY(ix*devWidth/w,iy*devHeight/h);
		}

		public RenderView(Context context)
		{
			super(context);
			P = new Point();
			pixelpaint = new Paint();
			pixelpaint.setStrokeCap(Paint.Cap.ROUND);
			pixelpaint.setStrokeWidth(0);
			pixelpaint.setColor(Color.RED);
			pixelpaint.setStyle(Paint.Style.FILL);
		}

		protected void onDraw(Canvas canvas)
		{
			//canvas.drawPoint(x, y, pixelpaint);
			canvas.drawCircle(P.getX(), P.getY(), 10, pixelpaint);
			invalidate();
		}
	}

	public class TestReceiver extends Receiver {


		/**
		 * Method extracts x,y, height and width from the received message.
		 * */
		public void extractCoordinatesFromMessage(String msg) {

			String X = msg.substring(msg.indexOf("X")+1, msg.indexOf("Y"));
			String Y = msg.substring(msg.indexOf("Y")+1,msg.indexOf("H"));
			String H = msg.substring(msg.indexOf("H")+1,msg.indexOf("W"));
			String W = msg.substring(msg.indexOf("W")+1);

			float x=200,y=200,h=rv.getHeight(),w=rv.getWidth();
			Log.i("X", X);
			Log.i("Y", Y);
			Log.i("H", H);
			Log.i("W", W);
			Log.i("X&Y&H&W", msg);
			try
			{
				x = Float.valueOf(X.trim()).floatValue();
				y = Float.valueOf(Y.trim()).floatValue();
				h = Float.valueOf(H.trim()).floatValue();
				w = Float.valueOf(W.trim()).floatValue();

			}
			catch (NumberFormatException nfe)
			{
				nfe.printStackTrace();
			}

			rv.setXandYRelative(x,y,h,w);
		}
		public void receive(Message msg) {

			for (String key : msg.getMeta().keySet()) {
				//Log.d("Touch", key + ": " + msg.getMeta(key));
			}
			try {
				String rec_message = new String(msg.getData(), "UTF-8");
				Log.d("RECEIVED MESSAGEXXX", rec_message);
				if(rec_message.contains("joined")) {

					/**
					 * Message is send as "X23Y34.0H200W300" without quotes where 23,34,200,300 are
					 * x,y, height and width respectively.
					 * */
					String message = "X"+String.valueOf(rv.getX())+"Y"+String.valueOf(rv.getY())+"H"+String.valueOf(rv.getHeight())+"W"+rv.getWidth();
					fooPub.send(message.getBytes());
				} else {
					extractCoordinatesFromMessage(rec_message);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rv = new RenderView(this);

		setContentView(rv);

		rv.setXandY(200,200);

		rv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				String message = "X"+String.valueOf(event.getX())+"Y"+String.valueOf(event.getY())+"H"+String.valueOf(rv.getHeight())+"W"+rv.getWidth();
				Log.d("Touch X", String.valueOf(event.getX()));
				Log.d("Touch Y", String.valueOf(event.getY()));
				rv.setXandY(event.getX(),event.getY());
				fooPub.send(message.getBytes());
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

   		String message = new String("joined");
		fooPub.send(message.getBytes());
	}
}
