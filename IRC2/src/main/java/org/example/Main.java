
package org.example;

import com.iconloop.score.token.irc2.IRC2;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public class Main implements IRC2 {

    private static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);
    private final VarDB<String> name= Context.newVarDB("token_name",String.class); //since cant instantiate String type like this. Init is necessary since we made it final. Final made so as to not to change location on bc
    private final VarDB<String> symbol= Context.newVarDB("token_symbol",String.class);
    private final VarDB<BigInteger> decimal=Context.newVarDB("token_decimals", BigInteger.class);
    private final VarDB<BigInteger> totalSupply=Context.newVarDB("token_supply", BigInteger.class);
    private final DictDB<Address,BigInteger> balances=Context.newDictDB("token_balances", BigInteger.class);
    //ownwer=>(spender=>allowance)
    private final BranchDB<Address,DictDB<Address,BigInteger>> allowances= Context.newBranchDB("allowances", BigInteger.class);

    public Main(String _name,String _symbol, int _decimals,BigInteger _totalSupply){
        Context.require(_name!=null && !_name.trim().isEmpty(),"Name cannot be empty or null!");
        Context.require(_symbol!=null && !_symbol.trim().isEmpty(),"Symbol cannot be empty!");
        this.name.set(_name);
        this.symbol.set(_symbol);

        Context.require(_decimals>=0 && _decimals<=21,"Decimals out of range!");
        this.decimal.set(BigInteger.valueOf(_decimals));

        _mint(Context.getCaller(),_totalSupply.multiply(pow10(_decimals)));
    }

    private BigInteger pow10(int _supply){
        BigInteger total= BigInteger.ONE;
        for(int i=1;i<=_supply;i++){
            total=total.multiply(BigInteger.TEN);
        }
//        Context.println("Deth TOTAL="+String.valueOf(total));
        return total;
    }

    @External(readonly = true)
    public String name() {
        return this.name.get();
    }

    @External(readonly = true)
    public String symbol() {
        return this.symbol.get();
    }

    @External(readonly = true)
    public BigInteger decimals() {
        return this.decimal.get();
    }

    @External(readonly = true)
    public BigInteger totalSupply() {
        return this.totalSupply.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        return this.balances.getOrDefault(_owner,BigInteger.ZERO);
    }

    @External
    public void approve(Address _to, BigInteger allowance){
        Address _from=Context.getCaller();
        Context.require(!_to.equals(ZERO_ADDRESS),"Cannot approve zero address");
        Context.require(allowance.compareTo(BigInteger.ZERO)>0,"Allowance must be posiive");
        Context.require(balanceOf(_from).compareTo(allowance)>=0,"Insufficient balance to allow!");
        this.allowances.at(_from).set(_to,allowance);
    }

    private BigInteger getAllowance(Address _owner){
        return this.allowances.at(_owner).getOrDefault(Context.getCaller(),BigInteger.ZERO);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Address _from=Context.getCaller();
        Context.require(_value.compareTo(BigInteger.ZERO)>0,"Cannot transfer 0 or negative tokens!");
        Context.require(balanceOf(_from).compareTo(_value)>=0,"Insufficent balance");

        balances.set(_from,balanceOf(_from).subtract(_value));
        balances.set(_to,balanceOf(_to).add(_value));
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        Transfer(_from,_to,_value,dataBytes);

        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", _from, _value, dataBytes);
        }
    }

    private void _decreaseAllowance(Address _owner,Address _spender, BigInteger _value){
        DictDB<Address,BigInteger> allowance=this.allowances.at(_owner);
        BigInteger oldAllowance=allowance.get(_spender);
        allowance.set(_spender,oldAllowance.subtract(_value));
    }
    @External
    public void transferFrom(Address _from,Address _to, BigInteger _value, @Optional byte[] _data){
        Context.require(_value.compareTo(BigInteger.ZERO)>0,"Can only transfer positive tokens!");
        Context.require(getAllowance(_from).compareTo(_value)>0,"Not Approved for the given value");
        Context.require(!_to.equals(ZERO_ADDRESS),"Cannot send to zero address!");
        Context.require(balanceOf(_from).compareTo(_value)>=0,"Insufficient balance!");

        balances.set(_from,balanceOf(_from).subtract(_value));
        balances.set(_to,balanceOf(_to).add(_value));
        _decreaseAllowance(_from,Context.getCaller(),_value);
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        Transfer(_from,_to,_value,dataBytes);

        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", _from, _value, dataBytes);
        }
    }

    private void _mint(Address owner, BigInteger _value){
        Context.require(_value.compareTo(BigInteger.ZERO)>0,"Cannot mint 0 or negative tokens!");
        Context.require(!owner.equals(ZERO_ADDRESS),"Cannot mint to zero address!");

        balances.set(owner,balanceOf(owner).add(_value));
        totalSupply.set(totalSupply().add(_value));
        Transfer(ZERO_ADDRESS,owner,_value,"mint".getBytes());

    }

    private void _burn(Address from,BigInteger _value){
        Context.require(_value.compareTo(BigInteger.ZERO)>0,"Cannot burn 0 or negative tokens!");
        Context.require(!from.equals(ZERO_ADDRESS),"Cannot burn from zero address!");
        Context.require(balanceOf(from).compareTo(_value)>=0,"Insufficient Balance");

        balances.set(from,balanceOf(from).subtract(_value));
        totalSupply.set(totalSupply().subtract(_value));
        Transfer(from,ZERO_ADDRESS,_value,"burn".getBytes());
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    }
}