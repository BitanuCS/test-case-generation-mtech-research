import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Runs one JUnit test class and prints one line per test method:
 *   ##PASS <method>
 *   ##FAIL <method> <one-line failure message>
 * plus a short stack trace (lines inside the test class only) after each FAIL.
 */
public class TestRunner {
    public static void main(String[] args) throws Exception {
        final String className = args[0];
        Class<?> cls = Class.forName(className);
        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            private final java.util.Set<String> failed = new java.util.HashSet<>();

            @Override
            public void testFailure(Failure f) {
                String name = f.getDescription().getMethodName();
                failed.add(name);
                Throwable t = f.getException();
                String msg = (t.getClass().getName() + ": " + t.getMessage()).replace('\n', ' ');
                if (msg.length() > 500) msg = msg.substring(0, 500) + "...";
                System.out.println("##FAIL " + name + " " + msg);
                for (StackTraceElement e : t.getStackTrace()) {
                    if (e.getClassName().startsWith(className)) {
                        System.out.println("    at " + e);
                    }
                }
            }

            @Override
            public void testFinished(Description d) {
                if (!failed.contains(d.getMethodName())) {
                    System.out.println("##PASS " + d.getMethodName());
                }
            }
        });
        core.run(Request.aClass(cls));
        System.exit(0);
    }
}
