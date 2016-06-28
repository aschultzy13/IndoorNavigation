package com.atp.rerc.indoornavigation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Geocoder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Extends great ImageView library by Dave Morrissey. See more:
 * https://github.com/davemorrissey/subsampling-scale-image-view.
 */
public class BlueDotView extends SubsamplingScaleImageView {

    int destinationX = 1200;    // horizontal position of upper left corner of destination icon
    int destinationY = 1060;    // vertical position of upper left corner of destination icon
    private float radius = 0.5f;
    private PointF dotCenter = null;
    public PointF destinationPoint = new PointF(destinationX, destinationY);
    Barcode.GeoPoint p;

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setDotCenter(PointF dotCenter) {
        this.dotCenter = dotCenter;
    }

    public BlueDotView(Context context) {
        this(context, null);
    }

    public BlueDotView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {
        setWillNotDraw(false);
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // display destination icon on canvas
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.desticon);
        bitmap = Bitmap.createScaledBitmap(bitmap, 40, 40, false);
        canvas.drawBitmap(bitmap, destinationX, destinationY, null);
//        canvas.drawBitmap(bitmap,(canvas.getWidth() / 2 - 25),(canvas.getHeight() / 2 - 25), null);


        if (!isReady()) {
            return;
        }

        // display blue dot on canvas
        if (dotCenter != null) {
            PointF vPoint = sourceToViewCoord(dotCenter);
            float scaledRadius = getScale() * radius;
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getResources().getColor(R.color.ia_blue));
            canvas.drawCircle(vPoint.x, vPoint.y, scaledRadius, paint);

            System.out.println("dot center:" + dotCenter);
            System.out.println("destination point:" + destinationPoint);
            // when tablet thinks it is at the destination X show stop sign
//            if (dotCenter == destinationPoint) {
            if(destinationPoint.equals(dotCenter)) {
                Intent i = new Intent(getContext(), ReachedDestination.class);
                getContext().startActivity(i);
            }


        }
    }










}

