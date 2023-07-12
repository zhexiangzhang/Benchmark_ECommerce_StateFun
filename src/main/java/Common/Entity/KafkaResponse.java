package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaResponse {
    @JsonProperty("taskId")
    private long taskId;
    @JsonProperty("tid")
    private int tid;
    @JsonProperty("receiver") // tell stream which worker to send to
    private String receiver;
    @JsonProperty("result")
    private String result;

    @JsonCreator
    public KafkaResponse(@JsonProperty("taskId") long taskId,
                         @JsonProperty("tid") int tid,
                         @JsonProperty("receiver") String receiver,
                         @JsonProperty("result") String result
    ) {
        this.taskId = taskId;
        this.tid = tid;
        this.receiver = receiver;
        this.result = result;
    }
}
