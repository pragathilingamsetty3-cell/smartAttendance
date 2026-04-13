import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.FileWriter;
import java.util.List;

@Component
public class SessionIDFetcher implements CommandLineRunner {
    private final ClassroomSessionV1Repository repository;

    public SessionIDFetcher(ClassroomSessionV1Repository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<ClassroomSession> sessions = repository.findAll();
        try (FileWriter writer = new FileWriter("active_sessions.txt")) {
            for (ClassroomSession s : sessions) {
                if (s.isActive()) {
                    writer.write("SESSION_ID:" + s.getId() + ",SUBJECT:" + s.getSubject() + ",SECTION:" + s.getSection().getId() + "\n");
                }
            }
        }
    }
}
