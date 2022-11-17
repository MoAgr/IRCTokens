import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.example.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.Address;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class IRC2Test extends TestBase {
    private static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);
    private static final String name= "DETHEREUM";
    private static final String symbol= "DETH";
    private static final  int decimal=18;
    private static final  BigInteger initialSupply=BigInteger.valueOf(1000);
    private static final BigInteger totalSupply = initialSupply.multiply(BigInteger.TEN.pow(decimal));
    private static final ServiceManager sm=getServiceManager();
    private static final Account owner=sm.createAccount();
    private Score ircToken;

    @BeforeEach
    public void setup() throws Exception{
        ircToken=sm.deploy(owner, Main.class,"DETHEREUM","DETH",18,BigInteger.valueOf(1000));
    }

    @Test
    void name() {
        assertEquals(name, ircToken.call("name"));
    }

    @Test
    void symbol() {
        assertEquals(symbol, ircToken.call("symbol"));
    }

    @Test
    void decimals() {
        assertEquals(BigInteger.valueOf(decimal), ircToken.call("decimals"));
    }

    @Test
    void balanceOf(){
        assertEquals(totalSupply,ircToken.call("balanceOf",owner.getAddress()));
    }

    @Test
    void totalSupply(){
        assertEquals(totalSupply,ircToken.call("totalSupply"));
    }

    private void approveAcc(Account _from, Address _to, BigInteger _allowance){
        ircToken.invoke(_from,"approve",_to,_allowance);
    }

    @Test
    void transfer(){
        var alice=sm.createAccount();
        ircToken.invoke(owner,"transfer",alice.getAddress(),BigInteger.valueOf(1000),"Trf".getBytes());
        assertEquals(BigInteger.valueOf(1000),ircToken.call("balanceOf",alice.getAddress()));
    }

    @Test
    void transferFrom(){
        var alice=sm.createAccount();
        var bob=sm.createAccount();
        approveAcc(owner,alice.getAddress(),BigInteger.valueOf(500));
        ircToken.invoke(alice,"transferFrom",owner.getAddress(),bob.getAddress(),BigInteger.valueOf(500),"Trf".getBytes());
        assertEquals(BigInteger.valueOf(500),ircToken.call("balanceOf",bob.getAddress()));
    }
}
