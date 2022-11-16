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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class IRC3Test extends TestBase {
    private static final BigInteger mintCost = new BigInteger("1000000000000000000");
    private static final String name="DENFT";
    private static final String symbol="DN";
    private static final ServiceManager sm=getServiceManager();
    private static final Account owner=sm.createAccount();
    private Score ircToken;
    private Main tokenSpy;

    @BeforeEach
    public void setup() throws Exception{
        ircToken=sm.deploy(owner, Main.class,"DENFT","DN");
        tokenSpy= (Main) spy(ircToken.getInstance());
        ircToken.setInstance(tokenSpy);
    }

    @Test
    void name() {
        assertEquals(name, ircToken.call("name"));
    }

    @Test
    void symbol() {
        assertEquals(symbol, ircToken.call("symbol"));
    }

    private BigInteger mint(BigInteger _tokenId){
        doReturn(mintCost).when(tokenSpy).getPaidValue();
        ircToken.invoke(owner,"mint",_tokenId);
        return _tokenId;
    }

    @Test
    void balanceOf(){
        mint(BigInteger.valueOf(1));
        assertEquals(1,ircToken.call("balanceOf",owner.getAddress()));
    }

    @Test
    void ownerOf(){
        var id=mint(BigInteger.valueOf(1));
        assertEquals(owner.getAddress(),ircToken.call("ownerOf",id));
    }

    private void approveAcc(Account _from,Address _to,BigInteger _tokenId){
        ircToken.invoke(_from,"approve",_to,_tokenId);
    }

    @Test
    void approve(){
        var id=mint(BigInteger.valueOf(1));
        var alice=sm.createAccount();
        approveAcc(owner,alice.getAddress(),id);
        assertEquals(alice.getAddress(),ircToken.call("getApproved",id));
    }

    @Test
    void transfer(){
        var id=mint(BigInteger.valueOf(1));
        var alice=sm.createAccount();
        ircToken.invoke(owner,"transfer",alice.getAddress(),id);
        assertEquals(alice.getAddress(),ircToken.call("ownerOf",id));
    }

    @Test
    void transferFrom(){
        var id=mint(BigInteger.valueOf(1));
        var alice=sm.createAccount();
        var bob=sm.createAccount();
        approveAcc(owner,alice.getAddress(),id);
        ircToken.invoke(alice,"transferFrom",owner.getAddress(),bob.getAddress(),id);
        assertEquals(bob.getAddress(),ircToken.call("ownerOf",id));
    }

    @Test
    void burn(){
        var id=mint(BigInteger.valueOf(1));
        var alice=sm.createAccount();
        ircToken.invoke(owner,"transfer",alice.getAddress(),id);
        assertThrows(AssertionError.class, () ->
                     ircToken.invoke(owner,"burn",id));
        assertDoesNotThrow(()->ircToken.invoke(alice,"burn",id));
    }

}
