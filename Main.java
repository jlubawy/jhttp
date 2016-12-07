
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;

import java.net.ServerSocket;
import java.net.Socket;

class Main {
    public static void main(String[] args) {
        final HttpResponse resp;

        // Decide what content-type to return in the HTTP response (default to 'html')
        final String contentType = (args.length == 1) ? args[0] : "html";
        switch (contentType) {
            case "html":
                resp = new HTMLResponse("<!DOCTYPE html><html><head><title>Hello World</title></head><body>Hello World</body></html>");
                break;

            case "json":
                resp = new JSONResponse("{\"message\": \"Hello World!\"}");
                break;

            case "text":
                resp = new PlainTextResponse("Hello World!");
                break;

            default:
                throw new IllegalArgumentException("unknown content type '" + contentType + "'");
        }

        try (final ServerSocket serverSocket = new ServerSocket(80)) {
            // Start server loop (single-threaded)
            while (true) {
                try (
                    final Socket clientSocket = serverSocket.accept();
                    final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ) {
                    String firstLine = "";
                    String line;

                    System.out.println("New connection from: " + clientSocket.getInetAddress().toString() + "\n");

                    // Read and log data from the socket
                    boolean isFirstLine = true;
                    System.out.println("    Received input:\n");
                    while ((line = in.readLine()) != null) {
                        if (isFirstLine) {
                            if (!line.equals("GET / HTTP/1.1")) {
                                firstLine = line;
                            }

                            isFirstLine = false;
                        }

                        System.out.println("        " + line + "\\r\\n");

                        if (line.equals("")) {
                            break; // empty line signals end of headers (content would normally come after but we ignore it in this case)
                        }
                    }

                    String respString = (firstLine.equals("")) ? resp.getResponseString() : (new NotFoundHttpResponse(firstLine)).getResponseString();

                    try (
                        final BufferedReader respReader = new BufferedReader(new StringReader(respString));
                    ) {
                        // Log and write data to the socket
                        System.out.println("\n    Writing output:\n");
                        while ((line = respReader.readLine()) != null) {
                            System.out.println("        " + line + "\\r\\n");
                        }
                        System.out.println("");
                        out.print(respString);
                        out.close();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static abstract class HttpResponse {
        protected final String statusCode;
        protected final String content;

        public HttpResponse(String content) {
            this("200 OK", content);
        }

        public HttpResponse(String statusCode, String content) {
            this.statusCode = statusCode;
            this.content = (content != null) ? content : "";
        }

        public String getResponseString() {
            return "HTTP/1.1 " + statusCode + "\r\n"
                 + "Content-Length: " + content.getBytes().length + "\r\n"
                 + "Content-Type: " + getContentType() + "; charset=UTF-8\r\n"
                 + "Connection: Closed\r\n"
                 + "\r\n"
                 + content;
        }

        public abstract String getContentType();
    }

    private static class NotFoundHttpResponse extends HttpResponse {
        public NotFoundHttpResponse(String content) {
            super("404 Not Found", String.format("Resource not found: '%s'", content));
        }

        public String getContentType() {
            return "text/plain";
        }
    }

    private static class HTMLResponse extends HttpResponse {
        public HTMLResponse(String content) {
            super(content);
        }

        public String getContentType() {
            return "text/html";
        }
    }

    private static class JSONResponse extends HttpResponse {
        public JSONResponse(String content) {
            super(content);
        }

        public String getContentType() {
            return "application/json";
        }
    }

    private static class PlainTextResponse extends HttpResponse {
        public PlainTextResponse(String content) {
            super(content);
        }

        public String getContentType() {
            return "text/plain";
        }
    }
}
