package com.lopez.digitalreader;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import android.gesture.Gesture;

import com.lopez.digitalreader.MarshmallowPermissions.Permissions;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener /*, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener*/ {

    private ImageView viewPDF;
    private int currentPage = 0;

    private String fileName;
    private static final int FILE_OPEN_CODE = 1;
    private Permissions permissions = new Permissions(this);

    //VARIABLES FOR PDF RENDERER
    private String filePath = "";
    private PdfRenderer renderer;
    private Bitmap bitmap;
    private Rect rect;
    private float x1, x2, y1, y2;
    private Uri selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        viewPDF = (ImageView) findViewById(R.id.imgPDFViewer);
        viewPDF.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = MotionEventCompat.getActionMasked(motionEvent);

                switch (action){
                    case (MotionEvent.ACTION_MOVE):
                        /*Toast.makeText(getApplicationContext(), "ACTION_MOVE",
                                Toast.LENGTH_SHORT).show();*/
                        //Log.d("DEBUG_EVENT","ACTION_MOVE. X = " + x1);
                        return true;
                    case (MotionEvent.ACTION_DOWN):
                        /*Toast.makeText(getApplicationContext(), "ACTION_DOWN",
                                Toast.LENGTH_SHORT).show();*/
                        x1 = motionEvent.getX();
                        y1 = motionEvent.getY();
                        Log.d("DEBUG_EVENT","ACTION_DOWN. X = " + x1);
                        return true;
                    case (MotionEvent.ACTION_UP):
                        /*Toast.makeText(getApplicationContext(), "ACTION_UP",
                                Toast.LENGTH_SHORT).show();*/
                        x2 = motionEvent.getX();
                        y2 = motionEvent.getY();
                        Log.d("DEBUG_EVENT","ACTION_UP. X = " + x2);

                        if (x1 < x2){
                            if (currentPage > 0 ){
                                currentPage--;
                                Log.d("DEBUG_EVENT","Moved to previous page");
                            }
                        }
                        if (x1 > x2){
                            if (currentPage < renderer.getPageCount() - 1){
                                currentPage++;
                                Log.d("DEBUG_EVENT","Moved to next page");
                            }

                        }

                        try {
                            Log.d("DEBUG_EVENT","Moving to page " + (currentPage + 1));
                            movePage(currentPage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return true;
                }
                return false;
            }
        });
/*        gestureDetectorCompat = new GestureDetectorCompat(this, this);
        gestureDetectorCompat.setOnDoubleTapListener(this);*/

        if (!permissions.checkPermissionsForExternalStorage()) {
            permissions.requestPermissionForExternalStorage();
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_open) {
            Intent openFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            openFileIntent.setType("*/*");
            startActivityForResult(openFileIntent, FILE_OPEN_CODE);

        } else if (id == R.id.nav_recent) {

        } else if (id == R.id.nav_favorites) {

        } else if (id == R.id.nav_store) {

        } else if (id == R.id.nav_donate) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_OPEN_CODE && resultCode == RESULT_OK && data != null) {
            selectedFile = data.getData();
            fileName = data.toString();
            try {
                openFile(selectedFile, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void openFile(Uri fileUri, String stringFromUri) throws IOException {

        final int WIDTH = viewPDF.getWidth();
        final int HEIGHT = viewPDF.getHeight();
        currentPage = 0;

        String ID = "";

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(getApplicationContext(), fileUri)) {
            if (isExternalStorageDocument(fileUri)) {
                final String DOC_ID = DocumentsContract.getDocumentId(fileUri);
                final String[] SPLIT = DOC_ID.split(":");
                final String type = SPLIT[0];
                if ("primary".equalsIgnoreCase(type)) {
                    filePath = Environment.getExternalStorageDirectory() + "/" + SPLIT[1];
                }
            } else if (isDownloadsDocument(fileUri)) {
                ID = DocumentsContract.getDocumentId(fileUri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(ID));
                filePath = getDataColumn(getApplicationContext(), contentUri, null, null);
            }
        }

        bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_4444);
        File pdfFile = new File(filePath);
        //Toast.makeText(getApplicationContext(), "Width: " + WIDTH + " Height: " + HEIGHT, Toast.LENGTH_LONG).show();
        //Toast.makeText(getApplicationContext(), "Opened: " + filePath, Toast.LENGTH_LONG).show();
        renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

        if (currentPage < 0) {
            currentPage = 0;
        } else if (currentPage > renderer.getPageCount()) {
            currentPage = renderer.getPageCount() - 1;
        }

        //Matrix matrix =  viewPDF.getImageMatrix();
        rect = new Rect(0, 0, WIDTH, HEIGHT);
        renderer.openPage(currentPage).render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        //viewPDF.setImageMatrix(matrix);
        viewPDF.setImageBitmap(bitmap);
        viewPDF.invalidate();
        viewPDF.setAdjustViewBounds(true);
        viewPDF.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public void movePage(int currentPage) throws IOException{
        final int WIDTH = viewPDF.getWidth();
        final int HEIGHT = viewPDF.getHeight();

        String ID = "";

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(getApplicationContext(), selectedFile)) {
            if (isExternalStorageDocument(selectedFile)) {
                final String DOC_ID = DocumentsContract.getDocumentId(selectedFile);
                final String[] SPLIT = DOC_ID.split(":");
                final String type = SPLIT[0];
                if ("primary".equalsIgnoreCase(type)) {
                    filePath = Environment.getExternalStorageDirectory() + "/" + SPLIT[1];
                }
            } else if (isDownloadsDocument(selectedFile)) {
                ID = DocumentsContract.getDocumentId(selectedFile);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(ID));
                filePath = getDataColumn(getApplicationContext(), contentUri, null, null);
            }
        }

        bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        File pdfFile = new File(filePath);
        //Toast.makeText(getApplicationContext(), "Width: " + WIDTH + " Height: " + HEIGHT, Toast.LENGTH_LONG).show();
        //Toast.makeText(getApplicationContext(), "Opened: " + filePath, Toast.LENGTH_LONG).show();
        renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

        if (currentPage < 0) {
            currentPage = 0;
        } else if (currentPage > renderer.getPageCount()) {
            currentPage = renderer.getPageCount() - 1;
        }

        //Matrix matrix =  viewPDF.getImageMatrix();
        rect = new Rect(0, 0, WIDTH, HEIGHT);
        renderer.openPage(currentPage).render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        //viewPDF.setImageMatrix(matrix);
        viewPDF.setImageBitmap(bitmap);
        viewPDF.invalidate();
        viewPDF.setAdjustViewBounds(true);
        viewPDF.setScaleType(ImageView.ScaleType.CENTER_CROP);
        //viewPDF.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Nullable
    private String getDataColumn(Context applicationContext, Uri contentUri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String COLUMN = "_data";
        final String[] PROJECTION = {COLUMN};
        try {
            cursor = applicationContext.getContentResolver().query(contentUri, PROJECTION, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int INDEX = cursor.getColumnIndexOrThrow(COLUMN);
                String dataColumn = cursor.getString(INDEX);
                cursor.close();
                return dataColumn;
            }
        } catch (Exception e) {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
}
