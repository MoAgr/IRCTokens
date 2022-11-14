package org.example;

import com.iconloop.score.token.irc3.IRC3;
import com.iconloop.score.util.IntSet;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;

public class Main implements IRC3 {

    private static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);
    private static final BigInteger mintCost = new BigInteger("1000000000000000000");
    private final VarDB<String> name= Context.newVarDB("token_name",String.class);
    private final VarDB<String> symbol= Context.newVarDB("token_symbol",String.class);
    //owner=>set of tokens
    private final DictDB<Address, IntSet> holderTokens = Context.newDictDB("holder_tokens", IntSet.class);
    //token_id=>owner
    private final DictDB<BigInteger,Address> tokenHolder = Context.newDictDB("token_holders", Address.class);
    //token_id=>approved_addr
    private final DictDB<BigInteger,Address> approvals= Context.newDictDB("approvals", Address.class);

    public Main(String _name,String _symbol){
        Context.require(_name!=null && !_name.trim().isEmpty(),"Name cannot be empty or null!");
        Context.require(_symbol!=null && !_symbol.trim().isEmpty(),"Symbol cannot be empty!");
        this.name.set(_name);
        this.symbol.set(_symbol);
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
    public int balanceOf(Address _owner) {
        Context.require(!_owner.equals(ZERO_ADDRESS),"Cannot read balance of zero address");
        IntSet tokens=holderTokens.get(_owner);
        return (tokens==null)?0:tokens.length();
    }

    @External(readonly = true)
    public Address ownerOf(BigInteger _tokenId) {
        return tokenHolder.getOrDefault(_tokenId,ZERO_ADDRESS);
    }

    @External(readonly = true)
    public Address getApproved(BigInteger _tokenId) {
        return approvals.getOrDefault(_tokenId,ZERO_ADDRESS);
    }

    @External
    public void approve(Address _to, BigInteger _tokenId) {
        Address _owner=Context.getCaller();
        Context.require(_owner.equals(ownerOf(_tokenId)),"Not the owner!");
        approvals.set(_tokenId,_to);
        Approval(_owner,_to,_tokenId);
    }

    @External
    public void transfer(Address _to, BigInteger _tokenId) {
        Address _from=Context.getCaller();
        Context.require(ownerOf(_tokenId).equals(_from),"Access denied.");
        Context.require(!_to.equals(ZERO_ADDRESS),"Cannot transfer to zero address.");

        tokenHolder.set(_tokenId,_to);
        IntSet receiverTokens=holderTokens.get(_to);
        if(receiverTokens==null){
            receiverTokens=new IntSet(_to.toString());
        }
        receiverTokens.add(_tokenId);
        holderTokens.set(_to,receiverTokens);

        IntSet giverTokens=holderTokens.get(_from);
        giverTokens.remove(_tokenId);
        holderTokens.set(_from,giverTokens);

        Transfer(_from,_to,_tokenId);
    }

    @External
    public void transferFrom(Address _from, Address _to, BigInteger _tokenId) {
        Address caller = Context.getCaller();
        Context.require(getApproved(_tokenId).equals(caller) || caller.equals(ownerOf(_tokenId)),"Not Approved to transfer!");
        Context.require(_from.equals(ownerOf(_tokenId)),"From address is not the owner.");
        Context.require(!_to.equals(ZERO_ADDRESS),"Cannot transfer to zero address.");

        tokenHolder.set(_tokenId,_to);
        IntSet receiverTokens=holderTokens.get(_to);
        receiverTokens.add(_tokenId);
        holderTokens.set(_to,receiverTokens);

        IntSet giverTokens=holderTokens.get(_from);
        giverTokens.remove(_tokenId);

        Transfer(_from,_to,_tokenId);
    }

    @External @Payable
    public void mint(BigInteger _tokenId){
        Address caller=Context.getCaller();
//        Context.require(!ZERO_ADDRESS.equals(caller), "Destination address cannot be zero address");
        Context.require(!_tokenExists(_tokenId), "Token already exists");
        Context.require(Context.getValue().equals(mintCost), "Please pay 1 icx to mint.");

        _addTokenTo(_tokenId, caller);
        tokenHolder.set(_tokenId, caller);
        Transfer(ZERO_ADDRESS, caller, _tokenId);
    }

    @External
    public void burn(BigInteger _tokenId){
        Address owner=ownerOf(_tokenId);
        Context.require(Context.getCaller().equals(Context.getOwner())); //only owner can mint

        approvals.set(_tokenId,ZERO_ADDRESS);
        _removeTokenFrom(_tokenId, owner);
        tokenHolder.set(_tokenId,ZERO_ADDRESS);
        Transfer(owner, ZERO_ADDRESS, _tokenId);
    }

    @EventLog(indexed =3)
    public void Transfer(Address _from, Address _to, BigInteger _tokenId) {
    }

    @EventLog(indexed = 3)
    public void Approval(Address _owner, Address _approved, BigInteger _tokenId) {
    }

    private boolean _tokenExists(BigInteger _tokenId) {
        return !ownerOf(_tokenId).equals(ZERO_ADDRESS);
    }

    private void _addTokenTo(BigInteger _tokenId, Address to){
        var tokens = holderTokens.get(to);
        if (tokens == null) {
            tokens = new IntSet(to.toString());
            holderTokens.set(to, tokens);
        }
        tokens.add(_tokenId);
    }

    private void _removeTokenFrom(BigInteger tokenId, Address from) {
        var tokens = holderTokens.get(from);
        Context.require(tokens != null, "Tokens don't exist for this address");
        tokens.remove(tokenId);
        if (tokens.length() == 0) {
            holderTokens.set(from, null);
        }
    }
}
