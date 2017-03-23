package json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author gluu
 * @since 23/03/17
 */
public class Output {

    @SerializedName("githubURL")
    @Expose
    private String githubURL;
    @SerializedName("quayioURL")
    @Expose
    private String quayioURL;
    @SerializedName("version")
    @Expose
    private String version;

    public String getGithubURL() {
        return githubURL;
    }

    public String getQuayioURL() {
        return quayioURL;
    }

    public String getVersion() {
        return version;
    }

    public void setGithubURL(String githubURL) {
        this.githubURL = githubURL;
    }

    public void setQuayioURL(String quayioURL) {
        this.quayioURL = quayioURL;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

