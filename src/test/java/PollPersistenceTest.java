import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.akila.surveyor.model.Poll;
import network.akila.surveyor.model.PollOption;
import network.akila.surveyor.model.Vote;
import network.akila.surveyor.persistence.dao.PollDAO;
import network.akila.surveyor.persistence.dao.PollOptionDAO;
import network.akila.surveyor.persistence.dao.VoteDAO;
import network.akila.surveyor.persistence.enums.DbType;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PollPersistenceTest {

    private HikariDataSource ds;
    private PollDAO pollDAO;
    private PollOptionDAO optionDAO;
    private VoteDAO voteDAO;

    @BeforeEach
    void setupPerTest(TestInfo info) throws Exception {
        String dbName = info.getDisplayName().replace(" ", "_");
        System.out.println("\n--- " + dbName + " ---");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared");
        config.setMaximumPoolSize(5);
        config.setConnectionTestQuery("SELECT 1");
        ds = new HikariDataSource(config);

        try (var c = ds.getConnection(); var st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON;");
        }

        pollDAO = new PollDAO(ds, DbType.SQLITE);
        optionDAO = new PollOptionDAO(ds, DbType.SQLITE);
        voteDAO = new VoteDAO(ds, DbType.SQLITE);

        System.out.println("Data source ready.");
    }

    @AfterEach
    void tearDown() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
            System.out.println("Data source closed.");
        }
    }

    @Test
    @DisplayName("Create a poll and load it back")
    void createAndRetrievePoll() {
        System.out.println("Create poll: 'Best System Administrator?' with two options.");
        Poll created = pollDAO.createPoll(
                "Best System Administrator?",
                Instant.now().plusSeconds(3600),
                List.of("Akila", "Others (Yikes)")
        );
        System.out.println("Poll id = " + created.getId());

        Poll loaded = pollDAO.findById(created.getId()).orElseThrow();
        System.out.println("Loaded question: " + loaded.getQuestion() + " | options: " + loaded.getOptions().size());

        assertNotNull(created.getId());
        assertEquals(created.getQuestion(), loaded.getQuestion());
        assertEquals(2, loaded.getOptions().size());
    }

    @Test
    @DisplayName("Fetch options for a poll")
    void insertAndRetrieveOptions() throws Exception {
        System.out.println("Create poll with options: Akila, No better one.");
        Poll poll = pollDAO.createPoll(
                "Best Java administrator?",
                Instant.now().plusSeconds(600),
                List.of("Akila", "No better one")
        );

        List<PollOption> options = optionDAO.findByPollId(poll.getId());
        System.out.println("Fetched " + options.size() + " options:");
        for (int i = 0; i < options.size(); i++) {
            System.out.println(" - [" + i + "] " + options.get(i).getText());
        }

        assertEquals(2, options.size());
        assertEquals("Akila", options.get(0).getText());
        assertEquals("No better one", options.get(1).getText());
    }

    @Test
    @DisplayName("Save votes and read them back")
    void saveAndRetrieveVotes() throws Exception {
        System.out.println("Create poll for voting.");
        Poll poll = pollDAO.createPoll(
                "Best System Administrator?",
                Instant.now().plusSeconds(600),
                List.of("Akila", "Others (Yikes)")
        );

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        System.out.println("Cast votes: " + u1 + " -> option 0, " + u2 + " -> option 1");

        Vote v1 = new Vote(poll.getId(), u1, 0, Instant.now());
        Vote v2 = new Vote(poll.getId(), u2, 1, Instant.now());

        voteDAO.upsertVote(v1);
        voteDAO.upsertVote(v2);

        var votes = voteDAO.findByPoll(poll.getId());
        System.out.println("Fetched " + votes.size() + " votes:");
        for (var v : votes) {
            System.out.println(" - voter=" + v.getPlayerUuid() + ", option=" + v.getOptionIndex());
        }

        assertEquals(2, votes.size());
    }

    @Test
    @DisplayName("List all polls")
    void findAllPolls() {
        System.out.println("Create two polls.");
        pollDAO.createPoll(
                "Best System Administrator?",
                Instant.now().plusSeconds(1000),
                List.of("Akila", "Others (Yikes)")
        );
        pollDAO.createPoll(
                "Best Java administrator?",
                Instant.now().plusSeconds(1000),
                List.of("Akila", "No better one")
        );

        List<Poll> polls = pollDAO.findAll();
        System.out.println("All polls (" + polls.size() + "):");
        for (Poll p : polls) {
            System.out.println(" - id=" + p.getId() + " | " + p.getQuestion() + " | options=" + p.getOptions().size());
        }

        assertEquals(2, polls.size());
    }
}
