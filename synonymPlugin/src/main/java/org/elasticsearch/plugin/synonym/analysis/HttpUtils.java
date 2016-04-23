package org.elasticsearch.plugin.synonym.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
 

public class HttpUtils{

	 
	
	public static void main(String args[]) throws IOException{
		System.out.println(doPost("http://192.168.0.254/system/synonym/synonym.dic"));
	}
	
	
	public static String doPost(String url) throws IOException{
		return doPost(url,null,10*1000,10*1000,null);
	}
	
 
	
	
	private static String doPost(String url, Map<String, String> params,
			int connectTimeout, int readTimeout, Map<String, String> headerMap) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		try {

			TreeMap<String, String> treeMap = new TreeMap<String, String>();
			if (params != null) {
				treeMap.putAll(params);
			}

			 
			String ctype = "application/x-www-form-urlencoded;charset=UTF-8";
			conn = getConnection(new URL(url), "POST", ctype, headerMap);
			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(readTimeout);
			
			String query = buildQuery(params);
			
			//System.out.println(query);
			byte[] content = {};
			if (query != null) {
				content = query.getBytes("UTF-8");
			}
			
			out = conn.getOutputStream();
			out.write(content);
			rsp = getResponseAsString(conn);

		} catch (IOException e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	private static HttpURLConnection getConnection(URL url, String method,
			String ctype, Map<String, String> headerMap) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "text/xml,text/javascript,text/html");
		conn.setRequestProperty("Content-Type", ctype);
		 if (headerMap != null) {
			 for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				 conn.setRequestProperty(entry.getKey(), entry.getValue());
			 }
		 }
		
		 
		
		return conn;
	}
 
	/**
	 * 判断字符串是否非空
	 * @param value	String
	 * @return boolean
	 */
	public static boolean isNotEmpty(String value) {
		return (value != null && !"".equals(value));
	}
	public static String buildQuery(Map<String, String> params)
			throws IOException {
		if (params == null || params.isEmpty()) {
			return null;
		}

		StringBuilder query = new StringBuilder();
		Set<Entry<String, String>> entries = params.entrySet();
		boolean hasParam = false;

		for (Entry<String, String> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue();
			// 忽略参数名或参数值为空的参数
			if (!isNotEmpty(name) || !isNotEmpty(value)) {
				if (hasParam) {
					query.append("&");
				} else {
					hasParam = true;
				}

				query.append(name).append("=")
						.append(URLEncoder.encode(value, "UTF-8"));
			}
		}

		return query.toString();
	}

 
   
	/*public static byte[] getFileContent(File file) throws IOException {
		byte[] content = null;

		if (file != null && file.exists()) {
			InputStream in = null;
			ByteArrayOutputStream out = null;

			try {
				in = new FileInputStream(file);
				out = new ByteArrayOutputStream();
				int ch;
				while ((ch = in.read()) != -1) {
					out.write(ch);
				}
				content = out.toByteArray();
			} finally {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			}
		}
		return content;
	}*/

	protected static String getResponseAsString(HttpURLConnection conn)
			throws IOException {
		InputStream es = conn.getErrorStream();
		if (es == null) {
			return getStreamAsString(conn.getInputStream(), "UTF-8");
		} else {
			String msg = getStreamAsString(es, "UTF-8");
			if (isNotEmpty(msg)) {
				throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());
			} else {
				throw new IOException(msg);
			}
		}
	}

	private static String getStreamAsString(InputStream stream, String charset)
			throws IOException {
		try {
			Reader reader = new InputStreamReader(stream, charset);
			StringBuilder response = new StringBuilder();

			final char[] buff = new char[1024];
			int read = 0;
			while ((read = reader.read(buff)) > 0) {
				response.append(buff, 0, read);
			}

			return response.toString();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

}