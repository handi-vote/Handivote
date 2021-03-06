import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

public class ReferendumFactory {

    private static Logger LOGGER = LoggerFactory.getLogger(ReferendumFactory.class);

    private VoteCollector setCollector(UUID refID, String voteCollectorType, int numberOfQuestions, ArrayList<Question> questions) {
        VoteCollector voteCollector;
        LOGGER.info("Collector type: " + voteCollectorType);
        switch (voteCollectorType) {
            case "mockEmail":
                voteCollector = new MockEmailCollector();
                break;
            case "Email":
                voteCollector = new EmailCollector(questions);
                break;
            case "SMS":
                voteCollector = new SMSCollector(questions);
                break;
            case "Test":
                voteCollector = new SimpleTestCollector(questions);
                break;
            default:
                IllegalArgumentException exception = new IllegalArgumentException("Invalid Collector type: " + voteCollectorType);
                LOGGER.error(exception.getMessage(), exception);
                throw exception;
        }
        return voteCollector;

    }

    Referendum buildReferendum(Properties properties) {
        UUID refID = UUID.randomUUID();
        int numberOfCards = Integer.parseInt(properties.getProperty("numberOfCards"));
        String numFile = properties.getProperty("numsFile");
        int pinDigits = Integer.parseInt(properties.getProperty("digits"));
        String password1 = properties.getProperty("password1");
        String password2 = properties.getProperty("password2");
        // load card numbers
        registerVoters(refID, numberOfCards, pinDigits, password1, password2, numFile);
        String refType = properties.getProperty("refType");
        ArrayList<Question> questions = loadQuestions(properties);

        Referendum referendum;
        int numberOfQuestions = Integer.parseInt(properties.getProperty("numberOfQuestions"));
        VoteCollector voteCollector = setCollector(refID, properties.getProperty("voteMethod"), numberOfQuestions, questions);
        switch (refType) {
            case "simple":
                referendum = new SimpleReferendum();
                referendum.createReferendum(refID, questions, voteCollector);
                break;
            default:
                throw new IllegalArgumentException("Invalid Referendum type: " + refType);

        }
        LOGGER.info("Created Referendum :"+ referendum.getRefID() + " Type: " + refType);
        return referendum;
    }


    private ArrayList<Question> loadQuestions(Properties properties) {
        ArrayList<Question> questions = new ArrayList<Question>();
        for (int i = 0; i < new Integer(properties.getProperty("numberOfQuestions")); i++) {
            ArrayList<QuestionOption> options = new ArrayList<>();
            for (int j = 1; j <= new Integer(properties.getProperty("numberOfOptions")); j++) {
                options.add(new QuestionOption("" + (i+1), "" + j, properties.getProperty("questionOption" + j)));
            }
            for (int n = 1; n <= Integer.parseInt(properties.getProperty("numberOfQuestions")); n++) {
                Question question = new Question(n, 0, properties.getProperty("question" + String.valueOf(n)), options);
                questions.add(question);
            }

        }
        return questions;
    }

    private void registerVoters(UUID refID, int numberOfCards, int pinDigits, String password1, String password2, String numFile) {
        GenerateCardNumbers gcn = new GenerateCardNumbers(refID, password1, password2, numberOfCards, pinDigits);
        //StoreCardNumbers scn = new StoreCardNumbers(refID, gcn.getNumsList());
        //System.out.println("numfile=./" + numFile);


        ClassLoader loader = ReferendumFactory.class.getClassLoader();
        URL myURL = loader.getResource(numFile);
        String path = myURL.getPath();
        path = path.replaceAll("%20", " ");

        StoreCardNumbers scn = null;
        if(numFile != null && !numFile.isEmpty()){
            try {
                scn = new StoreCardNumbers(refID, path);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        else {
            scn = new StoreCardNumbers(refID, gcn.getNumsList());
        }
    }

}//home/p/Projects/handivote/src/main/resources/emailnums.text
