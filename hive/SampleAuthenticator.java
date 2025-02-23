package temp;

import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.AuthenticationException;
import org.apache.hadoop.hive.metastore.MetaStorePasswdAuthenticationProvider;

public class SampleAuthenticator implements MetaStorePasswdAuthenticationProvider {

    private Map<String, String> userMap = new HashMap<>();

    public SampleAuthenticator() {
      init();
    }

    private void init(){
      userMap.put("hmsuser", "hmspasswd");
    }

    @Override
    public void authenticate(String user, String password) throws AuthenticationException {
      if(!userMap.containsKey(user)) {
        throw new AuthenticationException("Invalid user : " + user);
      }
      if(!userMap.get(user).equals(password)){
        throw new AuthenticationException("Invalid passwd : " + password);
      }
    }
}