package it.e_gueli.postemobi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.tidy.Tidy;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PosteView extends Activity {

	private final class PosteWebViewClient extends WebViewClient {

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,
				String url) {
			if (url.endsWith(".png") || url.endsWith(".gif") || url.endsWith(".jpg")) {
				InputStream is = getResources().openRawResource(R.drawable._1x1);
				WebResourceResponse resp = new WebResourceResponse("image/png",
						"octet-stream", is);
				Log.d(TAG, "sending empty image " + url);
				return resp;
			}
			
			if (url.endsWith(".css")) {
				InputStream is = new ByteArrayInputStream(new byte[0]);
				WebResourceResponse resp = new WebResourceResponse("text/css",
						"utf-8", is);
				Log.d(TAG, "sending empty CSS " + url);
				return resp;
			}
			
			if (url.endsWith("www.poste.it/")) {
				try {
					URL u = new URL(url);
					
					Tidy tidy = new Tidy();
					tidy.setXHTML(true);
					ByteArrayOutputStream tidyBaos = new ByteArrayOutputStream();
					tidy.parse(u.openStream(), tidyBaos);
					
					ByteArrayInputStream tidyBais = new ByteArrayInputStream(tidyBaos.toByteArray());
					
					Source xmlSource = new StreamSource(tidyBais);
					Source xsltSource = new StreamSource(getResources().openRawResource(R.raw.xslt_index));
					
		            TransformerFactory transFact = TransformerFactory.newInstance();
		            Transformer trans = transFact.newTransformer(xsltSource);
		            ByteArrayOutputStream output = new ByteArrayOutputStream();
		            StreamResult result = new StreamResult(output);
		            trans.transform(xmlSource, result);
		            InputStream is = new ByteArrayInputStream(output.toByteArray());
					WebResourceResponse resp = new WebResourceResponse("text/html",
							"utf-8", is);
					Log.d(TAG, "sending transformed HTML " + url);
					return resp;
				}
				catch(Exception e) {
					Log.w(TAG, Log.getStackTraceString(e));
					return null;
				}
			}
			
			/*
			 * div class="ls-container"
			 * div id=
			 * #w1338215201295
			 * #w1338389947274
			 * #w1341303337019
			 * #PagePeelSmall
			 * #PagePeelBig
			 * #header
			 * #contenitore-navigazione-index
			 * #contenitore-social
			 * #contenitore-informazioni
			 * #contenitore-footer-index
			 * #news-left
			 * #contatto-rapido-contenitore_index
			 * 
			 * CSS:
			 * #contenitore-notizie padding
			 */

			return super.shouldInterceptRequest(view, url);
		}

		@Override
		public void onLoadResource(WebView view, String url) {
			Log.d(TAG, "loading " + url);
			super.onLoadResource(view, url);
		}
	}

	private static final String TAG = PosteView.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_poste_view);
		
		clearCache(this, 0);

		WebView web = (WebView) findViewById(R.id.webView);
		WebSettings webSettings = web.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadWithOverviewMode(true);
		web.setWebViewClient(new PosteWebViewClient());
		web.loadUrl("http://www.poste.it/");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_poste_view, menu);
		return true;
	}
	
	//helper method for clearCache() , recursive
	//returns number of deleted files
	static int clearCacheFolder(final File dir, final int numDays) {

	    int deletedFiles = 0;
	    if (dir!= null && dir.isDirectory()) {
	        try {
	            for (File child:dir.listFiles()) {

	                //first delete subdirectories recursively
	                if (child.isDirectory()) {
	                    deletedFiles += clearCacheFolder(child, numDays);
	                }

	                //then delete the files and subdirectories in this dir
	                //only empty directories can be deleted, so subdirs have been done first
	                if (child.lastModified() < new Date().getTime() - numDays * DateUtils.DAY_IN_MILLIS) {
	                    if (child.delete()) {
	                        deletedFiles++;
	                    }
	                }
	            }
	        }
	        catch(Exception e) {
	            Log.e(TAG, String.format("Failed to clean the cache, error %s", e.getMessage()));
	        }
	    }
	    return deletedFiles;
	}

	/*
	 * Delete the files older than numDays days from the application cache
	 * 0 means all files.
	 */
	public static void clearCache(final Context context, final int numDays) {
	    Log.i(TAG, String.format("Starting cache prune, deleting files older than %d days", numDays));
	    int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numDays);
	    Log.i(TAG, String.format("Cache pruning completed, %d files deleted", numDeletedFiles));
	}
}
