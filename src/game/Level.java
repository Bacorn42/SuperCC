package game;

import emulator.SuperCC;
import util.ByteList;

import java.util.BitSet;
import java.util.LinkedList;

import static game.Tile.*;

public class Level extends SaveState {
    
    private static final int HALF_WAIT = 0, KEY = 1, CLICK = 2;
    
    public final int levelNumber, startTime;
    public final byte[] title, password, hint;
    public final short[] toggleDoors, portals;
    public final int[][] trapConnections, cloneConnections;
    private final byte[] startingState;

    private int rngSeed;
    private Step step;
    private ByteList moves = new ByteList();

    public Level(int levelNumber, byte[] title, byte[] password, byte[] hint, short[] toggleDoors, short[] portals,
                 int[][] trapConnections, BitSet traps, int[][] cloneConnections,
                 Layer layerBG, Layer layerFG, CreatureList monsterList, SlipList slipList,
                 Creature chip, int time, int chips, RNG rng, int rngSeed, Step step){

        super(layerBG, layerFG, monsterList, slipList, chip,
                time, chips, new short[4], new short[4], rng, NO_CLICK, traps);

        this.levelNumber = levelNumber;
        this.startTime = time;
        this.title = title;
        this.password = password;
        this.hint = hint;
        this.toggleDoors = toggleDoors;
        this.portals = portals;
        this.trapConnections = trapConnections;
        this.cloneConnections = cloneConnections;
        this.rngSeed = rngSeed;
        this.step = step;
        this.startingState = save();

        this.slipList.setLevel(this);
        this.monsterList.setLevel(this);
    }

    public Layer getLayerBG() {
        return layerBG;
    }
    public Layer getLayerFG() {
        return layerFG;
    }
    public int getTimer(){
        System.out.println("----");
        System.out.println(startTime);
        System.out.println(tickNumber);
        System.out.println("----");
        if (tickNumber == 0) return startTime;
        else return startTime - tickNumber + 1;                     // The first tick does not change the timer
    }
    public int getChipsLeft(){
        return chipsLeft;
    }
    public void setChipsLeft(int chipsLeft){
        this.chipsLeft = chipsLeft;
    }
    public Creature getChip(){
        return chip;
    }
    public int getRngSeed(){
        return rngSeed;
    }
    public Step getStep(){
        return step;
    }
    public short[] getKeys(){
        return keys;
    }
    public void setKeys(short[] keys){
        this.keys = keys;
    }
    public short[] getBoots(){
        return boots;
    }
    public void setBoots(short[] boots){
        this.boots = boots;
    }
    public CreatureList getMonsterList(){
        return monsterList;
    }
    public SlipList getSlipList(){
        return slipList;
    }
    public int[][] getTrapConnections(){
        return trapConnections;
    }
    public int[][] getCloneConnections(){
        return cloneConnections;
    }
    public ByteList getMoves(){
        return moves;
    }
    public void setMoves(ByteList moves){
        this.moves = moves;
    }
    public void setClick(int position){
        this.mouseClick = position;
    }
    
    public void reset(int rngSeed, Step step){
        load(startingState);
        this.rngSeed = rngSeed;
        rng.setRNG(rngSeed);
        this.step = step;
        moves = new ByteList();
    }
    
    protected void popTile(Position position){
        layerFG.set(position, layerBG.get(position));
        layerBG.set(position, FLOOR);
    }
    protected void insertTile(Position position, Tile tile){
        layerBG.set(position, layerFG.get(position));
        layerFG.set(position, tile);
    }
    
    private int moveType(byte b, boolean halfMove, boolean chipSliding){
        if (b <= 0 || b == SuperCC.WAIT){
            if (mouseClick != NO_CLICK && (chipSliding || halfMove)) return CLICK;
            else return HALF_WAIT;
        }
        else if (b == SuperCC.UP || b == SuperCC.LEFT || b == SuperCC.DOWN || b == SuperCC.RIGHT){
            return KEY;
        }
        else return HALF_WAIT;
    }
    
    private void moveChipSliding(){
        int direction = chip.getDirection();
        Tile bgTile = layerBG.get(chip.getPosition());
        if (bgTile.isFF()) chip.tick(new int[] {direction}, this);
        else chip.tick(chip.getSlideDirectionPriority(bgTile, rng), this);
    }
    
    private void moveChip(int[] directions){
        int oldPosition = chip.getIndex();
        for (int direction : directions) {
            if (chip.isSliding()) {
                if (!layerBG.get(chip.getPosition()).isFF()) continue;
                if (direction == chip.getDirection()) continue;
            }
            chip.tick(new int[] {direction}, this);
            if (chip.getIndex() != oldPosition) break;
        }
    }

    private void finaliseTraps(){
        for (int i = 0; i < trapConnections.length; i++){
            if (layerBG.get(trapConnections[i][0]) == BUTTON_BROWN){
                traps.set(i, true);
            }
            else if (layerBG.get(trapConnections[i][1]) != TRAP){
                traps.set(i, false);
            }
        }
    }

    private void initialiseSlidingMonsters(){
        for (Creature m : monsterList.list) m.setSliding(false);
        for (Creature m : slipList) m.setSliding(true);
    }
    
    // return: did it tick twice?
    public boolean tick(byte b, int[] directions){
        
        initialiseSlidingMonsters();
        tickNumber++;
        boolean isHalfMove = tickNumber % 2 == 0;
        int moveType = moveType(b, isHalfMove, chip.isSliding());
    
        if (tickNumber > 2 && !isHalfMove) monsterList.tick();
        
        if (chip.isDead()) return false;
        if (chip.isSliding()) moveChipSliding();
        if (chip.isDead()) return false;
        if (chip.isSliding() && !layerBG.get(chip.getPosition()).isFF()) b = SuperCC.WAIT;
        if (moveType == CLICK) moveChip(chip.seek(new Position(mouseClick)));
        if (chip.isDead()) return false;
        slipList.tick();
        if (chip.isDead()) return false;
        if (moveType == KEY) moveChip(directions);
        if (chip.isDead()) return false;

        monsterList.finalise();
        finaliseTraps();
        if (moveType == KEY || chip.getIndex() == mouseClick) mouseClick = NO_CLICK;
    
        return moveType == KEY && !isHalfMove && !chip.isSliding();
    }
    
}
