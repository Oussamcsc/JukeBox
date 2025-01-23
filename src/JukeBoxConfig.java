import java.io.Serializable;
/**
 * JukeBoxConfig - A container class for storing and serializing configuration settings. let's go
 */
public class JukeBoxConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String mediaFolder;

    public JukeBoxConfig(String mediaFolder) {
        this.mediaFolder = mediaFolder;
    }

    public String getMediaFolder() {
        return mediaFolder;
    }

    public void setMediaFolder(String mediaFolder) {
        this.mediaFolder = mediaFolder;
    }
}
