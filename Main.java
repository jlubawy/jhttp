
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;

import java.net.ServerSocket;
import java.net.Socket;

class Main {
    public static void main(String[] args) {
        final HttpResponse resp;

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

        final String respString = resp.getResponseString();

        try (final ServerSocket serverSocket = new ServerSocket(80)) {
            try (
                final Socket clientSocket = serverSocket.accept();
                final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                final BufferedReader respReader = new BufferedReader(new StringReader(respString));
            ) {
                String line;

                // Log data read from socket
                System.out.println("Received input:\n");
                while ((line = in.readLine()) != null) {
                    System.out.println("    " + line + "\\r\\n");
                    if (line.equals("")) {
                        break; // empty line signals end of headers (don't care about content right now)
                    }
                }

                System.out.println("\nWriting output:\n");
                while ((line = respReader.readLine()) != null) {
                    System.out.println("    " + line + "\\r\\n");
                }
                out.print(respString);
                out.close();

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static abstract class HttpResponse {
        protected final String content;

        public HttpResponse(String content) {
            this.content = content;
        }

        public String getResponseString() {
            return "HTTP/1.1 200 OK\r\n"
                 + "Content-Length: " + this.content.getBytes().length + "\r\n"
                 + "Content-Type: " + getContentType() + "; charset=UTF-8\r\n"
                 + "Connection: Closed\r\n"
                 + "\r\n"
                 + content;
        }

        public abstract String getContentType();
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
