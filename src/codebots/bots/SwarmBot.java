package codebots.bots;

import codebots.bot.CodeBot;
import codebots.bot.ReadonlyBot;
import codebots.gameobjects.FunctionType;
import codebots.gameobjects.IPAddress;
import codebots.gameobjects.Message;

public class SwarmBot extends CodeBot {
	private final String TEAM = "Swarming";


    @Override
    public IPAddress selectMessageRecipient() {
    	if(this.getFlag().equals(TEAM)) {
    		this.getVariables().add("TEAM", this.getFlag());
    	}
    	else {
    		this.getVariables().add("TEAM", "CORRUPTED");
    	}
    	int r = this.getRound() + 1;
    	this.getVariables().add("ROUND", ""+r);
    	//TODO: if on correct team, find an enemy, DDOS it.
    	//if not on correct team, inform allies
        return getAddressBook().getAddress(0);
    }

    private int getRound() {
    	int r = 0;
    	if(this.getVariables().has("ROUND")) {
	    	try {
	    		r = Integer.parseInt(this.getVariables().get("ROUND"));
	    	}
	    	catch(NumberFormatException e) {
	    		r = 0;
	    	}
    	}
    	return r;
	}

	@Override
    public Message sendMessage() {
        return new Message(Message.MessageType.INFORM);
    }

    @Override
    public void processMessage(IPAddress source, Message message) {

    }
    
    @Override
    public FunctionType selectFunctionToBlock() {
    	if(this.getFlag().equals(TEAM))
    		return FunctionType.SELECT_FUNCTION_TO_BLOCK;
    	else
    		return FunctionType.SELECT_MESSAGE_RECIPIENTS;
    }

    @Override
    public IPAddress selectAttackTarget() {
    	//TODO: find a corrupted ally or enemy to attack
    	//target corrupted allies first
        return getAddressBook().getAddress(0);
    }

    @Override
    public void readData(ReadonlyBot bot) {
    	if(bot.getVariables().get("TEAM").equals(TEAM)) {
    		//ally
    	}
    	else {
    		//enemy, neutral, or other
    	}
    }

    @Override
    public FunctionType selectFunctionToReplace() {
    	//TODO: replace (in order) Block, recipient, Flag, message, data, replace, target, process 
        return FunctionType.GET_FLAG;
    }

    @Override
    public String getFlag() {
        return TEAM;
    }
}
