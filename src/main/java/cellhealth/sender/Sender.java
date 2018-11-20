package cellhealth.sender;

/**
 * Created by Alberto Pascual on 22/06/15.
 */
public interface Sender {

    public boolean isConnected();
    public String getType();
    public void send(cellhealth.core.statistics.Stats stats);
    public void send(cellhealth.core.statistics.chStats.Stats chStats);
    public void init();
}
