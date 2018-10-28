package game;

import static game.Tile.*;
import static java.lang.Math.abs;

/**
 * Creatures are encoded as follows:
 *
 *       0 0    | 0 0 0 0 | 0 0 0 0 0 | 0 0 0 0 0
 *    DIRECTION | MONSTER |    ROW    |    COL
 */
public class Creature{

    public static final int  BUG             = 0b00_0000_0000000000,
                             FIREBALL        = 0b00_0001_0000000000,
                             PINK_BALL       = 0b00_0010_0000000000,
                             TANK_MOVING     = 0b00_0011_0000000000,
                             GLIDER          = 0b00_0100_0000000000,
                             TEETH           = 0b00_0101_0000000000,
                             WALKER          = 0b00_0110_0000000000,
                             BLOB            = 0b00_0111_0000000000,
                             PARAMECIUM      = 0b00_1000_0000000000,
                             TANK_STATIONARY = 0b00_1001_0000000000,
                             BLOCK           = 0b00_1010_0000000000,
                             CHIP            = 0b00_1011_0000000000,
                             CHIP_SLIDING    = 0b00_1101_0000000000,
                             DEAD            = 0b00_1111_0000000000;

    public static final int DIRECTION_UP    = 0b00_0000_0000000000,
                            DIRECTION_LEFT  = 0b01_0000_0000000000,
                            DIRECTION_DOWN  = 0b10_0000_0000000000,
                            DIRECTION_RIGHT = 0b11_0000_0000000000;
    // Mouse clicks are stored as 0b00_1111_<position>;

    static final int TURN_LEFT = DIRECTION_LEFT, TURN_RIGHT = DIRECTION_RIGHT,
                             TURN_AROUND = DIRECTION_DOWN, TURN_FORWARD = DIRECTION_UP;

    private Position position;
    private int monsterType;
    private int direction;
    private boolean sliding;

    // Direction-related methods

    protected int getDirection(){
        return direction;
    }
    protected void setDirection(int direction){
        this.direction = direction;
    }

    static int turnFromDir(int direction, int turn){
        return (turn+direction) & 0b11_0000_0000000000;
    }
    int[] turnFromDir(int[] turns){
        int[] dirs = new int[turns.length];
        for (int i = 0; i < turns.length; i++){
            dirs[i] = turnFromDir(direction, turns[i]);
        }
        return dirs;
    }

    protected void turn(int turn){
        direction = turnFromDir(direction, turn);
    }

    int[] getDirectionPriority(Creature chip, RNG rng){
        if (isSliding()) return turnFromDir(new int[] {TURN_FORWARD, TURN_AROUND});
        switch (monsterType){
            case BUG: return turnFromDir(new int[] {TURN_LEFT, TURN_FORWARD, TURN_RIGHT, TURN_AROUND});
            case FIREBALL: return turnFromDir(new int[] {TURN_FORWARD, TURN_RIGHT, TURN_LEFT, TURN_AROUND});
            case PINK_BALL: return turnFromDir(new int[] {TURN_FORWARD, TURN_AROUND});
            case TANK_STATIONARY: return new int[] {};
            case GLIDER: return turnFromDir(new int[] {TURN_FORWARD, TURN_LEFT, TURN_RIGHT, TURN_AROUND});
            case TEETH: return position.seek(chip.position);
            case WALKER:
                int[] directions = new int[] {TURN_LEFT, TURN_AROUND, TURN_RIGHT};
                rng.randomPermutation3(directions);
                return turnFromDir(new int[] {TURN_FORWARD, directions[0], directions[1], directions[2]});
            case BLOB:
                directions = new int[] {TURN_FORWARD, TURN_LEFT, TURN_AROUND, TURN_RIGHT};
                rng.randomPermutation4(directions);
                return turnFromDir(directions);
            case PARAMECIUM: return turnFromDir(new int[] {TURN_RIGHT, TURN_FORWARD, TURN_LEFT, TURN_AROUND});
            case TANK_MOVING: return new int[] {getDirection()};
        }
        return new int[] {};
    }
    public int[] seek(Position position){
        return this.position.seek(position);
    }
    
    private static int applySlidingTile(int direction, Tile tile, RNG rng){
        switch (tile){
            case FF_DOWN:
                return DIRECTION_DOWN;
            case FF_UP:
                return DIRECTION_UP;
            case FF_RIGHT:
                return DIRECTION_RIGHT;
            case FF_LEFT:
                return DIRECTION_LEFT;
            case FF_RANDOM:
                return rng.random4() << 14;
            case ICE_SLIDE_SOUTHEAST:
                if (direction == DIRECTION_UP) return DIRECTION_RIGHT;
                else if (direction == DIRECTION_LEFT) return DIRECTION_DOWN;
                else return direction;
            case ICE_SLIDE_NORTHEAST:
                if (direction == DIRECTION_DOWN) return DIRECTION_RIGHT;
                else if (direction == DIRECTION_LEFT) return DIRECTION_UP;
                else return direction;
            case ICE_SLIDE_NORTHWEST:
                if (direction == DIRECTION_DOWN) return DIRECTION_LEFT;
                else if (direction == DIRECTION_RIGHT) return DIRECTION_UP;
                else return direction;
            case ICE_SLIDE_SOUTHWEST:
                if (direction == DIRECTION_UP) return DIRECTION_LEFT;
                else if (direction == DIRECTION_RIGHT) return DIRECTION_DOWN;
                else return direction;
        }
        return direction;
    }
    int[] getSlideDirectionPriority(Tile tile, RNG rng){
        if (tile.isIce() || (isChip() && tile == TELEPORT)){
            int[] directions = turnFromDir(new int[]{TURN_FORWARD, TURN_AROUND});
            directions[0] = applySlidingTile(directions[0], tile, rng);
            directions[1] = applySlidingTile(directions[1], tile, rng);
            return directions;
        }
        else if (tile == TELEPORT) return new int[] {direction};
        else{
            return new int[] {applySlidingTile(getDirection(), tile, rng)};
        }
    }
    
    // MonsterType-related methods

    int getMonsterType(){
        return monsterType;
    }
    void setMonsterType(int monsterType){
        this.monsterType = monsterType;
    }
    void kill(){
        monsterType = DEAD;
    }
    boolean isAffectedByCB(){
        return monsterType == TEETH || monsterType == BUG || monsterType == PARAMECIUM;
    }
    boolean isChip(){
        return monsterType == DEAD || monsterType == CHIP || monsterType == CHIP_SLIDING;
    }
    boolean isMonster(){
        return monsterType <= TANK_STATIONARY;
    }
    boolean isBlock(){
        return monsterType == BLOCK;
    }
    boolean isTank() {
        return monsterType == TANK_MOVING || monsterType == TANK_STATIONARY;
    }
    public boolean isDead() {
        return monsterType == DEAD;
    }


    // Position-related methods

    public Position getPosition() {
        return position;
    }
    public int getX(){
        return position.getX();
    }
    public int getY(){
        return position.getY();
    }
    public int getIndex() {
        return position.getIndex();
    }
    Position move(int direction){
        return position.move(direction);
    }
    private void moveForwards(){
        position = position.move(direction);
    }


    // Sliding-related functions

    public boolean isSliding() {
        return monsterType == CHIP_SLIDING || sliding;
    }
    void setSliding(boolean sliding){
        this.sliding = sliding;
    }
    void setSliding(boolean sliding, Level level){
        if (this.sliding && !sliding){
            if (isBlock() && level.layerBG[getIndex()] == TRAP) return;
            if (isChip()) setMonsterType(CHIP);
            else level.slipList.remove(this);
        }
        else if (!this.sliding && sliding){
            if (isChip()) setMonsterType(CHIP_SLIDING);
            else level.slipList.add(this);
        }
        this.sliding = sliding;
    }

    /**
     * Get the Tile representation of this monster.
     * @return The Tile representation of this monster.
     */
    Tile toTile(){
        switch (monsterType){
            case BLOCK: return Tile.BLOCK;
            case CHIP_SLIDING: return fromOrdinal(CHIP_UP.ordinal() | (getDirection() >>> 14));
            case TANK_STATIONARY: return fromOrdinal(TANK_NORTH.ordinal() | (getDirection() >>> 14));
            default: return fromOrdinal((getMonsterType() >>> 8) + 0x40 | getDirection() >>> 14);
        }
    }

    private MoveFlags pushBlock(Creature block, Level level){
        if (block.sliding) {
            int blockDirection = block.getDirection();
            if (blockDirection == direction || turnFromDir(blockDirection, TURN_AROUND) == direction)
                return MoveFlags.FAIL;
        }
        Position newChipPosition = block.position.clone();
        MoveFlags blockFlags = block.tryMove(direction, level);
        if (blockFlags.moved){
            MoveFlags chipFlags = tryEnter(direction, level, newChipPosition.getIndex(), level.layerFG[newChipPosition.getIndex()]);
            if (chipFlags.moved) {
                if (blockFlags.pressedRedButton) {
                    int redButtonIndex;
                    for (redButtonIndex = 0; redButtonIndex < level.cloneConnections.length; redButtonIndex++) {
                        if (level.cloneConnections[redButtonIndex][0] == block.position.getIndex()) {
                            level.monsterList.addClone(level.cloneConnections[redButtonIndex][1]);
                        }
                    }
                }
    
                if (blockFlags.pressedBrownButton) {
                    for (int j = 0; j < level.trapConnections.length; j++) {
                        if (level.trapConnections[j][0] == block.position.getIndex()) {
                            level.traps.set(j, true);
                            break;
                        }
                    }
                }
            }
            return chipFlags.combineButtons(blockFlags);
        }
        return MoveFlags.FAIL;
    }
    
    private boolean canLeave(int direction, Tile tile, Level level){
        switch (tile){
            case THIN_WALL_UP: return direction != DIRECTION_UP;
            case THIN_WALL_RIGHT: return direction != DIRECTION_RIGHT;
            case THIN_WALL_DOWN: return direction != DIRECTION_DOWN;
            case THIN_WALL_LEFT: return direction != DIRECTION_LEFT;
            case THIN_WALL_DOWN_RIGHT: return direction != DIRECTION_DOWN && direction != DIRECTION_RIGHT;
            case TRAP:
                for (int i = 0; i < level.trapConnections.length; i++){
                    if (level.trapConnections[i][1] == getIndex()){
                        if (level.traps.get(i)) return true;
                    }
                }
                return false;
            default: return true;
        }
    }
    boolean canEnter(int direction, Tile tile, Level level){
        switch (tile) {
            case FLOOR: return true;
            case WALL: return false;
            case CHIP: return isChip();
            case WATER: return true;
            case FIRE: return getMonsterType() != BUG && getMonsterType() != WALKER;
            case HIDDENWALL_PERM: return false;
            case THIN_WALL_UP: return direction != DIRECTION_DOWN;
            case THIN_WALL_RIGHT: return direction != DIRECTION_LEFT;
            case THIN_WALL_DOWN: return direction != DIRECTION_UP;
            case THIN_WALL_LEFT: return direction != DIRECTION_RIGHT;
            case BLOCK: return false;
            case DIRT: return isChip();
            case ICE:
            case FF_DOWN: return true;
            case BLOCK_UP:
            case BLOCK_LEFT:
            case BLOCK_RIGHT:
            case BLOCK_DOWN: return false;
            case FF_UP:
            case FF_LEFT:
            case FF_RIGHT: return true;
            case EXIT: return !isMonster();
            case DOOR_BLUE: return isChip() && level.keys[0] > 0;
            case DOOR_RED: return isChip() && level.keys[1] > 0;
            case DOOR_GREEN: return isChip() && level.keys[2] > 0;
            case DOOR_YELLOW: return isChip() && level.keys[3] > 0;
            case ICE_SLIDE_SOUTHEAST: return direction == DIRECTION_UP || direction == DIRECTION_LEFT;
            case ICE_SLIDE_NORTHEAST: return direction == DIRECTION_DOWN || direction == DIRECTION_LEFT;
            case ICE_SLIDE_NORTHWEST: return direction == DIRECTION_DOWN || direction == DIRECTION_RIGHT;
            case ICE_SLIDE_SOUTHWEST: return direction == DIRECTION_UP || direction == DIRECTION_RIGHT;
            case BLUEWALL_FAKE: return isChip();
            case BLUEWALL_REAL: return false;
            case OVERLAY_BUFFER: return false;
            case THIEF: return isChip();
            case SOCKET: return isChip() && level.chipsLeft <= 0;
            case BUTTON_GREEN: return true;
            case BUTTON_RED: return true;
            case TOGGLE_CLOSED: return false;
            case TOGGLE_OPEN: return true;
            case BUTTON_BROWN:
            case BUTTON_BLUE:
            case BOMB:
            case TRAP: return true;
            case HIDDENWALL_TEMP: return false;
            case GRAVEL: return (!isMonster());
            case POP_UP_WALL: return isChip();
            case HINT: return true;
            case THIN_WALL_DOWN_RIGHT: return direction == DIRECTION_DOWN || direction == DIRECTION_RIGHT;
            case CLONE_MACHINE: return false;
            case FF_RANDOM: return !isMonster();
            case DROWNED_CHIP:
            case BURNED_CHIP:
            case BOMBED_CHIP:
            case UNUSED_36:
            case UNUSED_37:
            case ICEBLOCK_STATIC:
            case EXITED_CHIP:
            case EXIT_EXTRA_1:
            case EXIT_EXTRA_2: return false;
            case CHIP_SWIMMING_NORTH:
            case CHIP_SWIMMING_WEST:
            case CHIP_SWIMMING_SOUTH:
            case CHIP_SWIMMING_EAST: return !isChip();
            //monsters
            default: return isChip();
            case KEY_BLUE:
            case KEY_RED:
            case KEY_GREEN:
            case KEY_YELLOW:
            case BOOTS_WATER:
            case BOOTS_FIRE:
            case BOOTS_ICE:
            case BOOTS_SLIDE: return !isMonster();
            case CHIP_UP:
            case CHIP_LEFT:
            case CHIP_RIGHT:
            case CHIP_DOWN: return !isChip();
        }
    }
    private MoveFlags tryEnter(int direction, Level level, int newPositionIndex, Tile tile){
        switch (tile) {
            case FLOOR: return MoveFlags.SUCCESS;
            case WALL: return MoveFlags.FAIL;
            case CHIP:
                if (isChip()) {
                    level.chipsLeft--;
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case WATER:
                if (isChip()){
                    if (level.boots[0] == 0){
                        level.layerFG[newPositionIndex] = DROWNED_CHIP;
                        return MoveFlags.DIED;
                    } else return MoveFlags.SUCCESS;
                }
                if (isBlock()) {
                    level.layerFG[newPositionIndex] = DIRT;
                    return MoveFlags.DIED;
                }
                if (monsterType != GLIDER) return MoveFlags.DIED;
                return MoveFlags.SUCCESS;
            case FIRE:
                if (isChip()) {
                    if (level.boots[1] == 0){
                        level.layerFG[newPositionIndex] = BURNED_CHIP;
                        return MoveFlags.DIED;
                    }
                    else return MoveFlags.SUCCESS;
                }
                switch (getMonsterType()) {
                    case Creature.BLOCK:
                    case Creature.FIREBALL:
                        return MoveFlags.SUCCESS;
                    case Creature.BUG:
                    case Creature.WALKER:
                        return MoveFlags.FAIL;
                    default:
                        return MoveFlags.DIED;
                }
            case HIDDENWALL_PERM: return MoveFlags.FAIL;
            case THIN_WALL_UP: return new MoveFlags(direction != DIRECTION_DOWN);
            case THIN_WALL_RIGHT: return new MoveFlags(direction != DIRECTION_LEFT);
            case THIN_WALL_DOWN: return new MoveFlags(direction != DIRECTION_UP);
            case THIN_WALL_LEFT: return new MoveFlags(direction != DIRECTION_RIGHT);
            case BLOCK:
                if (isChip()){
                    for (Creature m : level.slipList) if (m.getIndex() == newPositionIndex) return pushBlock(m, level);
                    Creature block = new Creature(newPositionIndex, Tile.BLOCK);
                    return pushBlock(block, level);
                }
                return MoveFlags.FAIL;
            case DIRT:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case ICE:
                if (isChip() && level.getBoots()[2] > 0) return MoveFlags.SUCCESS;
                else return MoveFlags.SLIDE;
            case FF_DOWN:
                if (isChip() && level.getBoots()[3] > 0) return MoveFlags.SUCCESS;
                else return MoveFlags.SLIDE;
            case BLOCK_UP:
            case BLOCK_LEFT:
            case BLOCK_RIGHT:
            case BLOCK_DOWN: return MoveFlags.FAIL;
            case FF_UP:
            case FF_LEFT:
            case FF_RIGHT:
                if (isChip() && level.getBoots()[3] > 0) return MoveFlags.SUCCESS;
                else return MoveFlags.SLIDE;
            case EXIT:
                if (isBlock()) return MoveFlags.SUCCESS;
                if (isChip()){
                    level.layerFG[newPositionIndex] = EXITED_CHIP;
                    return MoveFlags.DIED;
                }
                return MoveFlags.FAIL;
            case DOOR_BLUE:
                if (isChip() && level.keys[0] > 0) {
                    level.keys[0] = (short) (level.keys[0] - 1);
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case DOOR_RED:
                if (isChip() && level.keys[1] > 0) {
                    level.keys[1] = (short) (level.keys[1] - 1);
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case DOOR_GREEN:
                if (isChip() && level.keys[2] > 0) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case DOOR_YELLOW:
                if (isChip() && level.keys[3] > 0) {
                    level.keys[3] = (short) (level.keys[3] - 1);
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case ICE_SLIDE_SOUTHEAST:
                if (direction == DIRECTION_UP || direction == DIRECTION_LEFT){
                    if (isChip() && level.getBoots()[2] > 0) return MoveFlags.SUCCESS;
                    else return MoveFlags.SLIDE;
                }
                else return MoveFlags.FAIL;
            case ICE_SLIDE_NORTHEAST:
                if(direction == DIRECTION_DOWN || direction == DIRECTION_LEFT){
                    if (isChip() && level.getBoots()[2] > 0) return MoveFlags.SUCCESS;
                    else return MoveFlags.SLIDE;
                }
                else return MoveFlags.FAIL;
            case ICE_SLIDE_NORTHWEST:
                if(direction == DIRECTION_DOWN || direction == DIRECTION_RIGHT){
                    if (isChip() && level.getBoots()[2] > 0) return MoveFlags.SUCCESS;
                    else return MoveFlags.SLIDE;
                }
                else return MoveFlags.FAIL;
            case ICE_SLIDE_SOUTHWEST:
                if(direction == DIRECTION_UP || direction == DIRECTION_RIGHT){
                    if (isChip() && level.getBoots()[2] > 0) return MoveFlags.SUCCESS;
                    else return MoveFlags.SLIDE;
                }
                else return MoveFlags.FAIL;
            case BLUEWALL_FAKE:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                else return MoveFlags.FAIL;
            case BLUEWALL_REAL:
                if (isChip()) level.layerFG[newPositionIndex] = WALL;
                return MoveFlags.FAIL;
            case OVERLAY_BUFFER: return MoveFlags.FAIL;
            case THIEF:
                if (isChip()) {
                    level.boots = new short[]{0, 0, 0, 0};
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case SOCKET:
                if (isChip() && level.chipsLeft <= 0) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case BUTTON_GREEN: return new MoveFlags(true, true, false, false, false, false, false, false);
            case BUTTON_RED: return new MoveFlags(true, false, true, false, false, false, false, false);
            case TOGGLE_CLOSED: return MoveFlags.FAIL;
            case TOGGLE_OPEN: return MoveFlags.SUCCESS;
            case BUTTON_BROWN: return new MoveFlags(true, false, false, true, false, false, false, false);
            case BUTTON_BLUE: return new MoveFlags(true, false, false, false, true, false, false, false);
            case TELEPORT: return new MoveFlags(true, false, false, false, false, true, false, true);
            case BOMB:
                if (!isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                }
                return MoveFlags.DIED;
            case TRAP: return MoveFlags.SUCCESS;
            case HIDDENWALL_TEMP:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = WALL;
                }
                return MoveFlags.FAIL;
            case GRAVEL: return new MoveFlags(!isMonster());
            case POP_UP_WALL:
                if (isChip()) {
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            case HINT: return MoveFlags.SUCCESS;
            case THIN_WALL_DOWN_RIGHT: return new MoveFlags(direction == DIRECTION_DOWN || direction == DIRECTION_RIGHT);
            case CLONE_MACHINE: return MoveFlags.FAIL;
            case FF_RANDOM:
                if (isChip() && level.getBoots()[3] > 0) return MoveFlags.SUCCESS;
                else if (isChip() || isBlock()) return MoveFlags.SLIDE;
                else return MoveFlags.FAIL;
            case DROWNED_CHIP:
            case BURNED_CHIP:
            case BOMBED_CHIP:
            case UNUSED_36:
            case UNUSED_37:
            case ICEBLOCK_STATIC:
            case EXITED_CHIP:
            case EXIT_EXTRA_1:
            case EXIT_EXTRA_2: return MoveFlags.FAIL;
            case CHIP_SWIMMING_NORTH:
            case CHIP_SWIMMING_WEST:
            case CHIP_SWIMMING_SOUTH:
            case CHIP_SWIMMING_EAST:
                if (!isChip()) {
                    level.chip.kill();
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
            default:                                    // Monsters
                if (isChip()) {
                    return MoveFlags.DIED;
                }
                return MoveFlags.FAIL;
            case KEY_BLUE:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.keys[0]++;
                }
                return MoveFlags.SUCCESS;
            case KEY_RED:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.keys[1]++;
                }
                return MoveFlags.SUCCESS;
            case KEY_GREEN:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.keys[2]++;
                }
                return MoveFlags.SUCCESS;
            case KEY_YELLOW:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.keys[3]++;
                }
                return MoveFlags.SUCCESS;
            case BOOTS_WATER:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.boots[0] = 1;
                }
                return new MoveFlags(!isMonster());
            case BOOTS_FIRE:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.boots[1] = 1;
                }
                return new MoveFlags(!isMonster());
            case BOOTS_ICE:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.boots[2] = 1;
                }
                return new MoveFlags(!isMonster());
            case BOOTS_SLIDE:
                if (isChip()) {
                    level.layerFG[newPositionIndex] = FLOOR;
                    level.boots[3] = 1;
                }
                return new MoveFlags(!isMonster());
            case CHIP_UP:
            case CHIP_LEFT:
            case CHIP_RIGHT:
            case CHIP_DOWN:
                if (!isChip()) {
                    level.getChip().kill();
                    return MoveFlags.SUCCESS;
                }
                return MoveFlags.FAIL;
        }
    }
    private MoveFlags tryMove(int direction, Level level){
        if (direction == -1) return MoveFlags.FAIL;
        setDirection(direction);
        if ((direction == DIRECTION_LEFT && getX() == 0) ||
            (direction == DIRECTION_RIGHT && getX() == 31) ||
            (direction == DIRECTION_UP && getY() == 0) ||
            (direction == DIRECTION_DOWN && getY() == 31)) return MoveFlags.FAIL;

        if (!canLeave(direction, level.layerBG[getIndex()], level)) return MoveFlags.FAIL;
        Position newPosition = move(direction);
        int newPositionIndex = newPosition.getIndex();
        Tile newTile = level.layerFG[newPositionIndex];
        if (!isChip() && newTile.isChip()) newTile = level.layerBG[newPositionIndex];
        if (newTile.isTransparent() && !canEnter(direction, level.layerBG[newPositionIndex], level)) return MoveFlags.FAIL;
        
        MoveFlags flags = tryEnter(direction, level, newPositionIndex, newTile);
        
        if (flags.moved) {
            level.popTile(getIndex());
            position = newPosition;
    
            if (flags.enteredPortal){
                int portalIndex;
                for (portalIndex = 0; true; portalIndex++){
                    if (level.portals[portalIndex] == getIndex()){
                        break;
                    }
                }
                int l = level.portals.length;
                int i = portalIndex;
                do{
                    i--;
                    if (i < 0) i += l;
                    position.setIndex(level.portals[i]);
                    if (level.layerFG[position.getIndex()] != TELEPORT) continue;
                    Position exitPosition = position.move(direction);
                    if (exitPosition.getX() < 0 || exitPosition.getX() > 31 ||
                        exitPosition.getY() < 0 || exitPosition.getY() > 31) continue;
                    Tile exitTile = level.layerFG[exitPosition.getIndex()];
                    if (exitTile.isTransparent()) exitTile = level.layerBG[exitPosition.getIndex()];
                    if (isChip() && exitTile == Tile.BLOCK){
                        Creature block = new Creature(direction, BLOCK, exitPosition);
                        if (canEnter(direction, level.layerBG[exitPosition.getIndex()], level) &&
                            block.canLeave(direction, level.layerBG[exitPosition.getIndex()], level)){
                            Position blockPushPosition = exitPosition.move(direction);
                            if (blockPushPosition.getX() < 0 || blockPushPosition.getX() > 31 ||
                                blockPushPosition.getY() < 0 || blockPushPosition.getY() > 31) continue;
                            if (block.canEnter(direction, level.layerFG[blockPushPosition.getIndex()], level)){
                                break;
                            }
                        }
                        pushBlock(block, level);
                    }
                    if (canEnter(direction, exitTile, level)) break;
                }
                while (i != portalIndex);
            }
    
            if (flags.sliding && !isMonster()) this.direction = applySlidingTile(direction, level.layerFG[getIndex()], level.rng);
            
            if (!flags.creatureDied) level.insertTile(getPosition(), toTile());
    
            if (level.layerBG[newPositionIndex] == POP_UP_WALL) level.layerBG[newPositionIndex] = WALL;
        }
        else{
            if (sliding && !isMonster()) this.direction = applySlidingTile(direction, level.layerBG[getIndex()], level.rng);
        }
    
        setSliding(flags.sliding, level);
    
        if (flags.creatureDied){
            if (isMonster()) level.monsterList.numDeadMonsters++;
            kill();
        }
        
        return flags;
    }

    void tick(int[] directions, Level level){
        Creature copy = clone();
        for (int newDirection : directions){
            
            MoveFlags flags = tryMove(newDirection, level);
            
            if (flags.pressedGreenButton){
                for (int i : level.toggleDoors) {
                    if (level.layerBG[i] == TOGGLE_OPEN) level.layerBG[i] = TOGGLE_CLOSED;
                    else if (level.layerBG[i] == TOGGLE_CLOSED) level.layerBG[i] = TOGGLE_OPEN;
                    if (level.layerFG[i] == TOGGLE_OPEN) level.layerFG[i] = TOGGLE_CLOSED;
                    else if (level.layerFG[i] == TOGGLE_CLOSED) level.layerFG[i] = TOGGLE_OPEN;
                }
            }
            
            if (flags.pressedRedButton){
                int redButtonIndex;
                for (redButtonIndex = 0; redButtonIndex < level.cloneConnections.length; redButtonIndex++){
                    if (level.cloneConnections[redButtonIndex][0] == getIndex()){
                        level.monsterList.addClone(level.cloneConnections[redButtonIndex][1]);
                    }
                }
            }
            
            if (flags.pressedBrownButton){
                for (int j = 0; j < level.trapConnections.length; j++){
                    if (level.trapConnections[j][0] == getIndex()){
                        level.traps.set(j, true);
                        break;
                    }
                }
            }
            
            if (flags.pressedBlueButton){
                for (Creature m : level.monsterList.list) {
                    if (m.isTank() && !m.isSliding()){
                        m.setMonsterType(TANK_MOVING);
                        m.turn(TURN_RIGHT);
                        level.layerFG[m.getIndex()] = m.toTile();
                        m.turn(TURN_RIGHT);
                    }
                }
            }
    
            if (flags.moved) return;
            
        }
        setSliding(copy.sliding, level);
        if (isTank()) setMonsterType(TANK_STATIONARY);
        else if (monsterType == TEETH){
            direction = directions[0];
            level.layerFG[getIndex()] = toTile();
        }
        else setDirection(copy.direction);
    }
    
    public Creature(int direction, int monsterType, Position position){
        this.direction = direction;
        this.monsterType = monsterType;
        this.position = position;
    }
    public Creature(int position, Tile tile){
        this.position = new Position(position);
        if (BLOCK_UP.ordinal() <= tile.ordinal() && tile.ordinal() <= BLOCK_RIGHT.ordinal()){
            direction = ((tile.ordinal() + 2) % 4) << 14;
            monsterType = BLOCK;
        }
        else{
            direction = (tile.ordinal() % 4) << 14;
            if (tile == Tile.BLOCK) monsterType = BLOCK;
            else monsterType = ((tile.ordinal() - 0x40) / 4) << 10;
        }
        if (monsterType == TANK_STATIONARY) monsterType = TANK_MOVING;
    }
    public Creature(int bitMonster){
        direction = bitMonster & 0b11_0000_0000000000;
        monsterType = bitMonster & 0b00_1111_0000000000;
        if (monsterType == CHIP_SLIDING) sliding = true;
        position = new Position(bitMonster & 0b00_0000_1111111111);
    }

    public int bits(){
        return direction | monsterType | getIndex();
    }

    @Override
    public Creature clone(){
        Creature c = new Creature(direction, monsterType, position);
        c.sliding = sliding;
        return c;
    }

    @Override
    public String toString(){
        if (monsterType == BLOCK) return "Monster BLOCK "+direction+" at position "+position;
        return "Monster "+toTile()+" at position "+position;
    }

}
