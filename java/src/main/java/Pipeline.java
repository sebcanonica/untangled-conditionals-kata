import dependencies.Config;
import dependencies.Emailer;
import dependencies.Logger;
import dependencies.Project;
import io.vavr.control.Either;

public class Pipeline {
    private final Config config;
    private final Emailer emailer;
    private final Logger log;

    public Pipeline(Config config, Emailer emailer, Logger log) {
        this.config = config;
        this.emailer = emailer;
        this.log = log;
    }

    public void run(Project project) {
        test(project)
                .flatMap(this::deploy)
                .map(p -> "Deployment completed successfully")
                .bimap(this::notify, this::notify);
    }

    private Either<String, Project> test(Project project) {
        if (project.hasTests()) {
            if ("success".equals(project.runTests())) {
                log.info("Tests passed");
                return Either.right(project);
            } else {
                log.error("Tests failed");
                return Either.left("Tests failed");
            }
        } else {
            log.info("No tests");
            return Either.right(project);
        }
    }

    private Either<String,Project> deploy(Project project) {
        if ("success".equals(project.deploy())) {
            log.info("Deployment successful");
            return Either.right(project);
        } else {
            log.error("Deployment failed");
            return Either.left("Deployment failed");
        }
    }

    private Void notify(String message) {
        if (config.sendEmailSummary()) {
            log.info("Sending email");
            emailer.send(message);
        } else {
            log.info("Email disabled");
        }
        return null;
    }
}
