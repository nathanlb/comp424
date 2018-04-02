package student_player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import boardgame.Board;
import boardgame.Move;
import boardgame.Player;
import tablut.GreedyTablutPlayer;
import tablut.RandomTablutPlayer;
import tablut.TablutBoardState;
import tablut.TablutMove;
import tablut.TablutPlayer;

/** A player file submitted by a student. */
public class StudentPlayer extends TablutPlayer {
	
	private static Random randomGenerator;
	private int role;
	private static Map<TablutBoardState, Node> state_node;
	private double b;
	private double c;
	private long max_time;
	private long now;
	
    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260686487");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    @SuppressWarnings("unchecked")
	public Move chooseMove(TablutBoardState boardState) {
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...

        c = 1;
        b = 0.1;
        if (state_node == null){
        	//if ((state_node = (Map<TablutBoardState, Node>) MyTools.loadFromXMLwithXStream()) == null)
        		state_node = new HashMap<TablutBoardState, Node>();
        }
        	
        max_time = 1900000;
        role = player_id;
        randomGenerator = new Random();

        //Move myMove = boardState.getRandomMove();
        Move myMove = monte_carlo_tree_search(boardState);

        // Return your move to be processed by the server.
        return myMove;
    }
    
    /**
     * Performs Monte Carlo Tree Search
     * @param Board state
     * @return Move to be executes
     */
    private TablutMove monte_carlo_tree_search(TablutBoardState boardState){
    	
    	Node root;
    	Node node_choice;
    	int num_sims = 0;
    	
    	if (state_node.containsKey(boardState))
    		root = state_node.get(boardState);
    	else
    		root = new Node(boardState, boardState.getAllLegalMoves().size(), null);
    	
    	now = System.nanoTime();
    	while ((System.nanoTime() - now)/1000 < max_time && root.moves_unfinished > 0){
    		node_choice = tree_policy(root);
    		Sim_Results results = simulate(node_choice.board_state);
    		back_propagation(node_choice, results);
    		num_sims++;
    	}
    	long time_elapsed = (System.nanoTime() - now)/1000;
    	
    	return best_action(root);
    }
    
    /**
     * Chose node according to tree policy
     * @param root node
     * @return chosen node according to tree policy
     */
    private Node tree_policy(Node root){
    	
    	Node curr_node = root;
    	ArrayList<TablutMove> legal_moves;
    	ArrayList<TablutMove> unexpanded_moves;
    	TablutMove move;
    	TablutBoardState next_state;
    	
    	while (root.moves_unfinished > 0){
    		legal_moves = curr_node.board_state.getAllLegalMoves();
    		
    		if (curr_node.board_state.getWinner() != Board.NOBODY){
    			curr_node.propagate_completion();
    			return curr_node;
    		}
    		else if (curr_node.children.size() < legal_moves.size()){
    			unexpanded_moves = get_new_moves(curr_node, legal_moves);
    			
    			move = get_random_move(unexpanded_moves);
    			//next_state = (TablutBoardState)deepClone(curr_node.board_state);
    			next_state = (TablutBoardState)curr_node.board_state.clone();
    			next_state.processMove(move);
    			
    			Node child = new Node(next_state, legal_moves.size(), move);
    			curr_node.add_child(child);
    			state_node.put(next_state, child);
    			
    			return child;
    		}
    		else{
    			curr_node = best_child(curr_node);
    		}
    	}
    	return curr_node;
    }
    
    /**
     * Chose best child according to tree values
     * @param node from which to choose
     * @return returns next node to chose
     */
    private Node best_child(Node n){
    	
    	boolean enemy_turn = (n.board_state.getTurnPlayer() != role);
    	Map<Node,Double> vals = new HashMap<Node,Double>();
    	
    	for (Node child : n.children){
    		double wins = child.get_wins();
    		double plays = child.get_plays();
    		double a_wins = 0; //child.get_amaf_plays();
    		double a_plays = 0; //child.get_amaf_plays();
    		
    		if (enemy_turn){
    			wins = plays - wins;
    			//a_wins = a_plays - a_wins;
    		}
    		
    		double parent_plays = n.get_plays();
    		double beta = n.get_beta(b);
    		
    		if (a_plays > 0){
    			double newval = (1 - beta) * (wins / plays) + beta * (a_wins / a_plays) + this.c * Math.sqrt(2 * Math.log(parent_plays) / plays);
    			vals.put(child, newval);
    		}
    		else{
    			double newval = (wins / plays) + this.c * Math.sqrt(2 * Math.log(parent_plays)/ plays);
    			vals.put(child, newval);
    		}
    	}
    	
    	Node best_choice = get_max_key(vals);
		return best_choice;
    }
    
    /**
     * Gets the key associated with max value from map
     * @param map of of nodes with associated values
     * @return node associated with max value
     */
    private Node get_max_key(Map<Node, Double> map){
    	Node max_node = null;
    	double max_val = Double.NEGATIVE_INFINITY;
    	
    	for (Map.Entry<Node, Double> entry : map.entrySet()){
    		if (entry.getValue() > max_val){
    			max_node = entry.getKey();
    			max_val = entry.getValue();
    		}
    	}
    	return max_node;
    }
    /**
     * Returns the best move to make from argument node
     * @param node n form which to choose
     * @return best move
     */
    private TablutMove best_action(Node n){
    	
    	double most_plays = Double.NEGATIVE_INFINITY;
    	double best_wins = Double.NEGATIVE_INFINITY;
    	ArrayList<TablutMove> best_actions = new ArrayList<TablutMove>();
    	
    	for (Node c : n.children){
    		double wins = c.get_wins();
    		double plays = c.get_plays();
    		
    		if (plays > most_plays){
    			most_plays = plays;
    			best_actions.clear();
    			best_actions.add(c.move); 
    		}
    		else if (plays == most_plays){
    			if (wins > best_wins){
    				best_wins = wins;
    				best_actions.clear();
        			best_actions.add(c.move);
    			}
    			else if (wins == best_wins){
    				best_actions.add(c.move);
    			}
    		}
    	}
    	return get_random_move(best_actions);
    }
    
    /**
     * Update nodes in tree with result
     * @param node n
     * @param simulation results
     */
    private void back_propagation(Node n, Sim_Results r){
    	// Update all nodes in path
    	while (n.parent != null){
    		n.plays++;
    		n.wins += r.result;
    		
    		n = n.parent;
    	}
    	// Update root node
    	n.plays++;
    	n.wins += r.result;
    }
    
    /**
     * Simulates game of Tablut and returns result
     * @param boardState
     * @return Results of simulation
     */
    private Sim_Results simulate(TablutBoardState boardState){
    	
    	Sim_Results results = new Sim_Results();
    	int winner = Board.NOBODY; 
    	ArrayList<TablutMove>legal_moves;
    	TablutMove picked;
    	
    	TablutBoardState state = (TablutBoardState)boardState.clone();
    	
    	while (true && (System.nanoTime() - now)/1000 < max_time){
    		
    		winner = state.getWinner();
    		
    		if (winner != Board.NOBODY){
    			if (winner == role)
    				results.result = 1;
    			else if (winner == getOpponent())
    				results.result = 0;
    			else
    				results.result = 0.5;
    			return results;		
    		}
    		
    		legal_moves = state.getAllLegalMoves();
    		if (legal_moves.size() == 0){
    			winner = getOpponent();
    			if (winner == role)
    				results.result = 1;
    			else if (winner == getOpponent())
    				results.result = 0;
    			else
    				results.result = 0.5;
    			return results;		
    		}
    		picked = get_random_move(legal_moves);
    		results.actions.get(state.getTurnPlayer()).add(picked);
    		
    		state.processMove(picked);
    	}
    	results.result = 0.5;
		return results;	
    }
    
    /**
     * Returns unexpanded moves from node
     * @param node
     * @param legal moves
     * @return list of unexpanded moves
     */
    
    private ArrayList<TablutMove> get_new_moves(Node n, ArrayList<TablutMove> moves){
    	ArrayList<TablutMove> unex_moves = new ArrayList<TablutMove>();
    	for (TablutMove m : moves){
    		if (!n.moves_expanded.contains(m)){
    			unex_moves.add(m);
    		}
    	}
    	return unex_moves;
    }
    
    /**
     * Returns random move from specified move list
     * @param move list
     * @return random move
     */
    private TablutMove get_random_move(ArrayList<TablutMove> l){
    	int index = randomGenerator.nextInt(l.size());
    	TablutMove m = l.get(index);
    	return m;
    }
    
    /**
     * Returns opponent role as integer
     * @return 
     */
    private int getOpponent(){
    	return (role == TablutBoardState.MUSCOVITE) ? TablutBoardState.SWEDE : TablutBoardState.MUSCOVITE;
    }
    
    /**
     * This method makes a "deep clone" of any Java object it is given.
     */
     public static Object deepClone(Object object) {
       try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(object);
         ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
         ObjectInputStream ois = new ObjectInputStream(bais);
         return ois.readObject();
       }
       catch (Exception e) {
         e.printStackTrace();
         return null;
       }
     }
    
     /**
      * Node class to be used in building monte carlo tree
      * @author Nathan
      *
      */
    class Node {
    	
    	TablutBoardState board_state;
		double plays;
		double wins;
		double amaf_plays;
		double amaf_wins;
		double moves_unfinished;
		HashSet<Move> moves_expanded;
		ArrayList<Node> children;
		Node parent;
		TablutMove move;	
		
    	public Node(TablutBoardState state, double num_children, TablutMove m){
    		board_state = state;
    		plays = 10;
    		wins = 10;
    		amaf_plays = 10;
    		amaf_wins = 0.5;
    		children = new ArrayList<Node>();
    		parent = null;
    		moves_expanded = new HashSet<Move>();
    		moves_unfinished = num_children;
    		move = m;	
    	}
    	
    	void propagate_completion(){
    		if (this.parent == null)
    			return;
    		
    		if (this.moves_unfinished > 0){
    			this.moves_unfinished--;
    		}
    		
    		this.parent.propagate_completion();	
    	}
    	
    	void add_child(Node c){
    		this.children.add(c);
    		this.moves_expanded.add(c.move);
    		c.parent = this;
    	}
    	
    	boolean has_children(){
    		return children.size() > 0;
    	}
    	
    	double get_wins(){
    		return this.wins;
    	}
    	
    	double get_plays(){
    		return this.plays;
    	}
    	
    	double get_amaf_wins(){
    		return this.amaf_wins;
    	}
    	
    	double get_amaf_plays(){
    		return this.amaf_plays;
    	}
    	
    	double get_beta(double b){
    		return this.amaf_plays / (this.plays + this.amaf_plays + 4 * this.plays * this.amaf_plays * Math.pow(b, 2));
    	}
    	
    }
    
    private static class Sim_Results {
    	double result;
    	ArrayList<TablutMove> player_actions;
    	ArrayList<TablutMove> opponent_actions;
    	ArrayList<ArrayList<TablutMove>> actions;
    	
    	public Sim_Results(){
    		player_actions = new ArrayList<TablutMove>();
    		opponent_actions = new ArrayList<TablutMove>();
    		actions = new ArrayList<ArrayList<TablutMove>>();
    		actions.add(player_actions);
    		actions.add(opponent_actions);
    	}
    }
    
 // For Debugging purposes only.
    public static void main(String[] args) {
        TablutBoardState b = new TablutBoardState();
        
        Player swede = new GreedyTablutPlayer("GreedySwede");
        swede.setColor(TablutBoardState.SWEDE);
        ((GreedyTablutPlayer) swede).rand = new Random(4);

        //Player swede = new RandomTablutPlayer("RandomSwede");
        //swede.setColor(TablutBoardState.SWEDE);
        
        //Player swede = new StudentPlayer();
        //swede.setColor(TablutBoardState.SWEDE);
        
        //Player muscovite = new GreedyTablutPlayer("PlayerMuscovite");
        //muscovite.setColor(TablutBoardState.MUSCOVITE);
        //((GreedyTablutPlayer) muscovite).rand = new Random(4);

        //Player muscovite = new RandomTablutPlayer("RandomMuscovite");
        //muscovite.setColor(TablutBoardState.MUSCOVITE);
        
        Player muscovite = new StudentPlayer();
        muscovite.setColor(TablutBoardState.MUSCOVITE);

        Player player = muscovite;
        while (!b.gameOver()) {
            Move m = player.chooseMove(b);
            b.processMove((TablutMove) m);
            player = (player == muscovite) ? swede : muscovite;
            System.out.println("\nMOVE PLAYED: " + m.toPrettyString());
            b.printBoard();
        }
        //MyTools.saveToXMLwithXStream(state_node);
        System.out.println(TablutMove.getPlayerName(b.getWinner()) + " WIN!");
    }
}