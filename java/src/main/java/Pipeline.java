import dependencies.Config;
import dependencies.Emailer;
import dependencies.Logger;
import dependencies.Project;

public class Pipeline {
    private final Config config;
    private final Emailer emailer;
    private final Logger log;

    public Pipeline(Config config, Emailer emailer, Logger log) {
        this.config = config;
        this.emailer = emailer;
        this.log = log;
    }

    interface Notifier {
        void sendNotification(String message);
    }

    class NotifierFactory {
        Notifier build(Config config) {
            if (config.sendEmailSummary()) return new EmailNotifier();
            else return new NullNotifier();
        }

        class NullNotifier implements Notifier {
            public void sendNotification(String message) {
                log.info("Email disabled");
            }
        }

        class EmailNotifier implements Notifier {
            public void sendNotification(String message) {
                log.info("Sending email");
                emailer.send(message);
            }
        }
    }

    public void run(Project project) {
        /*
            hT -V-> rT -V-> d -V-> sES -V-> m1
                                       -F-> lI
                              -F-> sES -V-> m2
                                       -F-> lI
                       -F->        sES -V-> m3
                                       -F-> lI
               -F->         d -V-> sES -V-> m1
                                       -F-> lI
                              -F-> sES -V-> m2
                                       -F-> lI
         */

        String status;
        if (project.hasTests()) {
            if ("success".equals(project.runTests())) {
                log.info("Tests passed");
                status = deploy(project);
            } else {
                log.error("Tests failed");
                status = "Tests failed";
            }
        } else {
            log.info("No tests");
            status = deploy(project);
        }

        new NotifierFactory().build(config).sendNotification(status);
    }

    private String deploy(Project project) {
        if ("success".equals(project.deploy())) {
            log.info("Deployment successful");
            return "Deployment completed successfully";
        } else {
            log.error("Deployment failed");
            return "Deployment failed";
        }
    }
}
