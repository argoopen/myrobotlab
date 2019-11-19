package org.myrobotlab.codec;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * handles all encoding and decoding of MRL messages or api(s) assumed context -
 * services can add an assumed context as a prefix
 * /api/returnEncoding/inputEncoding/service/method/param1/param2/ ...
 * 
 * xmpp for example assumes (/api/string/gson)/service/method/param1/param2/ ...
 * 
 * scheme = alpha *( alpha | digit | "+" | "-" | "." ) Components of all URIs: [
 * &lt;scheme&gt;:]&lt;scheme-specific-part&gt;[#&lt;fragment&gt;]
 * http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes
 * 
 * branch API test 5
 */
public class CodecUtils {

  public final static Logger log = LoggerFactory.getLogger(CodecUtils.class);
  
  public static class ApiDescription {
    String key;
    String path; // {scheme}://{host}:{port}/api/messages
    String exampleUri;
    String description;

    public ApiDescription(String key, String uriDescription, String exampleUri, String description) {
      this.key = key;
      this.path = uriDescription;
      this.exampleUri = exampleUri;
      this.description = description;
    }
  }
  
  public final static String PARAMETER_API = "/api/";
  public final static String PREFIX_API = "api";

  // mime-types
  public final static String MIME_TYPE_JSON = "application/json";

  private transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();
  private transient static Gson prettyGson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();

  private static boolean initialized = false;

  public final static String makeFullTypeName(String type) {
    if (type == null) {
      return null;
    }
    if (!type.contains(".")) {
      return String.format("org.myrobotlab.service.%s", type);
    }
    return type;
  }

  public static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>(
      Arrays.asList(Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class));

  public static final Set<String> WRAPPER_TYPES_CANONICAL = new HashSet<String>(
      Arrays.asList(Boolean.class.getCanonicalName(), Character.class.getCanonicalName(), Byte.class.getCanonicalName(), Short.class.getCanonicalName(),
          Integer.class.getCanonicalName(), Long.class.getCanonicalName(), Float.class.getCanonicalName(), Double.class.getCanonicalName(), Void.class.getCanonicalName()));

  @Deprecated /* use MethodCache */
  final static HashMap<String, Method> methodCache = new HashMap<String, Method>();

  /**
   * a method signature map based on name and number of methods - the String[]
   * will be the keys into the methodCache A method key is generated by input
   * from some encoded protocol - the method key is object name + method name +
   * parameter number - this returns a full method signature key which is used
   * to look up the method in the methodCache
   */
  final static HashMap<String, ArrayList<Method>> methodOrdinal = new HashMap<String, ArrayList<Method>>();

  final static HashSet<String> objectsCached = new HashSet<String>();

  public static final String capitalize(final String line) {
    return Character.toUpperCase(line.charAt(0)) + line.substring(1);
  }

  public final static <T extends Object> T fromJson(String json, Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public final static <T extends Object> T fromJson(String json, Class<?> generic, Class<?>... parameterized) {
    return gson.fromJson(json, getType(generic, parameterized));
  }

  public final static <T extends Object> T fromJson(String json, Type type) {
    return gson.fromJson(json, type);
  }

  public static Type getType(final Class<?> rawClass, final Class<?>... parameterClasses) {
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return parameterClasses;
      }

      @Override
      public Type getRawType() {
        return rawClass;
      }

      @Override
      public Type getOwnerType() {
        return null;
      }

    };
  }

  static public final byte[] getBytes(Object o) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
    ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
    os.flush();
    os.writeObject(o);
    os.flush();
    return byteStream.toByteArray();
  }

  static public final String getCallbackTopicName(String topicMethod) {
    // replacements
    if (topicMethod.startsWith("publish")) {
      return String.format("on%s", capitalize(topicMethod.substring("publish".length())));
    } else if (topicMethod.startsWith("get")) {
      return String.format("on%s", capitalize(topicMethod.substring("get".length())));
    }

    // no replacement - just pefix and capitalize
    // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
    return String.format("on%s", capitalize(topicMethod));
  }

  // TODO
  // public static Object encode(Object, encoding) - dispatches appropriately

  static final public String getMsgKey(Message msg) {
    if (msg.sendingMethod != null) {
      return String.format("%s.%s --> %s.%s(%s) - %d", msg.sender, msg.sendingMethod, msg.name, msg.method, CodecUtils.getParameterSignature(msg.data), msg.msgId);
    } else {
      return String.format("%s --> %s.%s(%s) - %d", msg.sender, msg.name, msg.method, CodecUtils.getParameterSignature(msg.data), msg.msgId);
    }
  }

  static final public String getParameterSignature(final Object[] data) {
    if (data == null) {
      return "";
    }

    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != null) {
        Class<?> c = data[i].getClass(); // not all data types are safe
        // toString() e.g.
        // SerializableImage
        if (c == String.class || c == Integer.class || c == Boolean.class || c == Float.class || c == MRLListener.class) {
          ret.append(data[i].toString());
        } else {
          String type = data[i].getClass().getCanonicalName();
          String shortTypeName = type.substring(type.lastIndexOf(".") + 1);
          ret.append(shortTypeName);
        }

        if (data.length != i + 1) {
          ret.append(",");
        }
      } else {
        ret.append("null");
      }

    }
    return ret.toString();

  }

  static public String getServiceType(String inType) {
    if (inType == null) {
      return null;
    }
    if (inType.contains(".")) {
      return inType;
    }
    return String.format("org.myrobotlab.service.%s", inType);
  }

  public static Message gsonToMsg(String gsonData) {
    return gson.fromJson(gsonData, Message.class);
  }

  /**
   * most lossy protocols need conversion of parameters into correctly typed
   * elements this method is used to query a candidate method to see if a simple
   * conversion is possible
   * 
   * @param clazz
   *          the class
   * @return true/false
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz) || clazz == String.class;
  }

  public static boolean isWrapper(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz);
  }

  public static boolean isWrapper(String className) {
    return WRAPPER_TYPES_CANONICAL.contains(className);
  }
  
  static public String toCamelCase(String s) {
    String[] parts = s.split("_");
    String camelCaseString = "";
    for (String part : parts) {
      camelCaseString = camelCaseString + toCCase(part);
    }
    return String.format("%s%s", camelCaseString.substring(0, 1).toLowerCase(), camelCaseString.substring(1));
  }

  static public String toCCase(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
  }

  public final static String toJson(Object o) {
    return gson.toJson(o);
  }

  static public void toJson(OutputStream out, Object obj) throws IOException {
    String json = null;
    json = gson.toJson(obj);
    if (json != null)
      out.write(json.getBytes());
  }

  public final static String toJson(Object o, Class<?> clazz) {
    return gson.toJson(o, clazz);
  }

  public static void toJsonFile(Object o, String filename) throws IOException {
    FileOutputStream fos = new FileOutputStream(new File(filename));
    fos.write(gson.toJson(o).getBytes());
    fos.close();
  }

  // === method signatures begin ===

  static public String toUnderScore(String camelCase) {
    return toUnderScore(camelCase, false);
  }

  static public String toUnderScore(String camelCase, Boolean toLowerCase) {

    byte[] a = camelCase.getBytes();
    boolean lastLetterLower = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < a.length; ++i) {
      boolean currentCaseUpper = Character.isUpperCase(a[i]);

      Character newChar = null;
      if (toLowerCase != null) {
        if (toLowerCase) {
          newChar = (char) Character.toLowerCase(a[i]);
        } else {
          newChar = (char) Character.toUpperCase(a[i]);
        }
      } else {
        newChar = (char) a[i];
      }

      sb.append(String.format("%s%c", (lastLetterLower && currentCaseUpper) ? "_" : "", newChar));
      lastLetterLower = !currentCaseUpper;
    }

    return sb.toString();

  }

  public static boolean tryParseInt(String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch (Exception e) {

    }
    return false;
  }

  public static String type(String type) {
    int pos0 = type.indexOf(".");
    if (pos0 > 0) {
      return type;
    }
    return String.format("org.myrobotlab.service.%s", type);
  }

  static final String JSON = "application/javascript";

  public static final String API_MESSAGES = "messages";
  public static final String API_SERVICE = "service";

  public static String getSimpleName(String serviceType) {
    int pos = serviceType.lastIndexOf(".");
    if (pos > -1) {
      return serviceType.substring(pos + 1);
    }
    return serviceType;
  }

  public static final String getSafeReferenceName(String name) {
    return name.replaceAll("[/ .-]", "_");
  }

  public static String toPrettyJson(Object ret) {
    return prettyGson.toJson(ret);
  }

  static public Object[] decodeArray(Object data) throws Exception {
    // ITS GOT TO BE STRING - it just has to be !!! :)
    String instr = (String) data;
    // array of Strings ? - don't want to double encode !
    Object[] ret = null;
    synchronized (data) {
      ret = gson.fromJson(instr, Object[].class);
    }
    return ret;
  }

  static public Message cliToMsg(String from, String to, String data) {
    return cliToMsg(null, from, to, data);
  }

  /**
   * This is the Cli encoder - it takes a line of text and generates the
   * appropriate msg from it to either invoke (locally) or sendBlockingRemote
   * (remotely)
   * 
   * <pre>
   * 
   * The expectation of this encoding is:
   *    if "/api/service/" is found - the end of that string is the starting point
   *    if "/api/service/" is not found - then the starting point of the string should be the service
   *      e.g "runtime/getUptime"
   * 
   * Important to remember getRequestURI is NOT decoded and getPathInfo is.
   * 
   * 
            
            Method              URL-Decoded Result           
            ----------------------------------------------------
            getContextPath()        no      /app
            getLocalAddr()                  127.0.0.1
            getLocalName()                  30thh.loc
            getLocalPort()                  8480
            getMethod()                     GET
            getPathInfo()           yes     /a?+b
            getProtocol()                   HTTP/1.1
            getQueryString()        no      p+1=c+dp+2=e+f
            getRequestedSessionId() no      S%3F+ID
            getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
            getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
            getScheme()                     http
            getServerName()                 30thh.loc
            getServerPort()                 8480
            getServletPath()        yes     /test?
            getParameterNames()     yes     [p 2, p 1]
            getParameter("p 1")     yes     c d
   * </pre>
   * 
   * @param contextPath
   *          - prefix to be added if supplied
   * 
   * @param from
   *          - sender
   * @param to
   *          - target service
   * @param cmd
   *          - cli encoded msg
   * @return
   */
  static public Message cliToMsg(String contextPath, String from, String to, String cmd) {
    Message msg = Message.createMessage(from, to, "ls", null);

    // because we always want a "Blocking/Return" from the cmd line - without a
    // subscription
    msg.setBlocking();

    /**
     * <pre>
     
     The key to this interface is leading "/" ...
     "/" is absolute path - dir or execute
     without "/" means runtime method - spaces and quotes can be delimiters
    
    "/"  -  list services
    "/{serviceName}" - list data of service
    "/{serviceName}/" - list methods of service
    "/{serviceName}/{method}" - invoke method
    "/{serviceName}/{method}/" - list parameters of method
    "/{serviceName}/{method}/p0/p1/p2" - invoke method with parameters
    
     or runtime
     {method}
     {method}/
     {method}/p01
     * 
     * 
     * </pre>
     */

    cmd = cmd.trim();

    // remove uninteresting api prefix
    if (cmd.startsWith("/api/service")) {
      cmd = cmd.substring("/api/service".length());
    }

    if (contextPath != null) {
      cmd = contextPath + cmd;
    }

    // assume runtime as 'default'
    if (msg.name == null) {
      msg.name = "runtime";
    }

    // two possibilities - either it begins with "/" or it does not
    // if it does begin with "/" its an absolute path to a dir, ls, or invoke
    // if not then its a runtime method

    if (cmd.startsWith("/")) {
      // ABSOLUTE PATH !!!
      String[] parts = cmd.split("/");

      if (parts.length < 3) {
        msg.method = "ls";
        msg.data = new Object[] { "\"" + cmd + "\"" };
        return msg;
      }

      // fix me diff from 2 & 3 "/"
      if (parts.length >= 3) {
        msg.name = parts[1];
        // prepare the method
        msg.method = parts[2].trim();

        // FIXME - to encode or not to encode that is the question ...
        Object[] payload = new Object[parts.length - 3];
        for (int i = 3; i < parts.length; ++i) {
          payload[i - 3] = parts[i];
        }
        msg.data = payload;
      }
      return msg;
    } else {
      // NOT ABOSLUTE PATH - SIMILAR TO EXECUTING IN THE RUNTIME /usr/bin path
      // (ie runtime methods!)
      // spaces for parameter delimiters ?
      String[] spaces = cmd.split(" ");
      // FIXME - need to deal with double quotes e.g. func A "B and C" D - p0 =
      // "A" p1 = "B and C" p3 = "D"
      msg.method = spaces[0];
      Object[] payload = new Object[spaces.length - 1];
      for (int i = 1; i < spaces.length; ++i) {
        // webgui will never use this section of code
        // currently the codepath is only excercised by InProcessCli
        // all of this methods will be "optimized" single commands to runtime (i think)
        // so we are going to error on the side of String parameters - other data types will have problems
        payload[i - 1] = "\"" + spaces[i] + "\"";
      }
      msg.data = payload;

      return msg;
    }
  }
  
  static public List<ApiDescription> getApis() {
    List<ApiDescription> ret = new ArrayList<>();
    ret.add(new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure"));
    ret.add(new ApiDescription("service", "{scheme}://{host}:{port}/api/service", "http://localhost:8888/api/service/runtime/getUptime",
        "An synchronous api useful for simple REST responses"));
    return ret;
  }
  
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      String json = CodecUtils.fromJson("test", String.class);
      log.info("json {}", json);
      json = CodecUtils.fromJson("a test", String.class);
      log.info("json {}", json);
      json = CodecUtils.fromJson("\"a/test\"", String.class);
      log.info("json {}", json);
      CodecUtils.fromJson("a/test", String.class);
      
    } catch(Exception e) {
      log.error("main threw", e);
    }
  }

}
