/* A preamble because everyone loves them.
To load it just load the robotOfTheYear class and try to ignore the 15 or so classes compiled along with it. Also play with
delay set to 0, otherwise it will be slow and sad.
For your convenience, if you play it, you can skip the intro with enter. Also awful bug to do with the way I did movement
and the way the maze-environment calls reset, but when you hit reset nothing will happen. Just move once and the game will
actually end.

Firstly, if you're reading this I am so sorry about the length of file, but since only one file submission is allowed
here is this mess.
I think the most interesting problems were updating graphics and holding up the program for movement but allowing graphics
processing.
For graphics updating there's a neat little eventbus and event in which you can redraw the maze, this had a little
annoyance that it assumes your resetting and places the robot at the start. So I just move the start on every maze drawn.

I originally foolishly thought robot.centre was for not moving the robot, but this just crashes the maze. So instead a bit
like a windup toy the inputhandler is waiting for the time to be up before it moves and the controller does one step. But
in-game events are still constantly updating meaning we don't get much stuttering.
Lastly, getting input was an interesting task and a took a little crash course in JFrame, but essentially i just
stick on a keylistener to every component in the window (which you can actually retrieve quite easily).

BIG NOTE: there is a consistent error in this game which is caused by the maze-environment not me I swear. Its to do
with the events and eventbus which I'm assuming uses some sort of threading? Either way this problem disappears on 0.5
speed but that's no fun to play a game.
Event 107 causes things to update in two classes which sometime happen out of order, but they share data. I think it happens
when MazeGridPanel starts setMaze, retrieving a size for the beenbefore array which is of the previous maze size since mazelogic
hasn't set the maze yet. But then finally mazeLogic does set the maze and now MazeGridPanel loops the new maze which is
too big for the old beenbefore grid. This error will only occur if maze2 is larger than maze1 which of course is what I
do for one of the events. But if all mazes are the same size then this error never occurs.
*/
import uk.ac.warwick.dcs.maze.generators.BlankGenerator;
import uk.ac.warwick.dcs.maze.generators.LoopyGenerator;
import uk.ac.warwick.dcs.maze.generators.PrimGenerator;
import uk.ac.warwick.dcs.maze.logic.EventBus;
import uk.ac.warwick.dcs.maze.logic.IRobot;
import uk.ac.warwick.dcs.maze.logic.Maze;
import uk.ac.warwick.dcs.maze.logic.RobotImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class RobotOfTheYear {
    EventHandler eventHandler = new EventHandler();
    InputHandler inputHandler = new InputHandler();

    long lastCrash;

    int pollRun = 0;
    static int steps = 0;

    public void controlRobot(IRobot robot) {
        if(robot.getRuns() == 0 && pollRun == 0){
            // Setting up frames/components to receive key inputs and a little log area
            Frame[] frames = JFrame.getFrames();
            Frame mainFrame = frames[0];
            JLabel dialogBox = new JLabel("", SwingConstants.CENTER);
            dialogBox.setMinimumSize(new Dimension(0, 100));
            dialogBox.setOpaque(true); // Allows background to be painted
            dialogBox.setVisible(true);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.fill = 1;
            gridBagConstraints.weighty = 2.0D;
            mainFrame.add(dialogBox, gridBagConstraints);
            mainFrame.setSize(800, 700);
            Component[] components =  frames[0].getComponents();
            for(int i = 0; i < components.length; i++ ) {
                components[i].setFocusable(true);
                components[i].requestFocus();
                components[i].addKeyListener(inputHandler);
            }
            eventHandler.dialogBox = dialogBox;
            eventHandler.inputHandler = inputHandler;
        }
        if (pollRun == 0){
            eventHandler.start(robot);
            Timer.tick(); // Marks the start
        }
        pollRun++;

        do{
            eventHandler.update(robot);
            collisionCheck(robot);
        }while(!inputHandler.move(robot));
        steps++;

        // We're about to move
        movementCollisionCheck(robot);

    }

    private void movementCollisionCheck(IRobot robot){
        // If robot is about to hit a wall and wait a sec if just crashed - the eventbus takes time
        if(robot.look(IRobot.AHEAD) == IRobot.WALL && Timer.getTimeElapsed() - lastCrash > 1000){
            EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(102, ""));
            // A little time to acknowledge you died
            eventHandler.crashed();
            lastCrash = Timer.getTimeElapsed();
            EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(100, new Point(1, 1)));
            eventHandler.nextEvent(robot);
        }
    }

    private void collisionCheck(IRobot robot){
        // If robot is inside a wall
        if(robot.getMaze().getCellType(robot.getLocation()) == 2 && Timer.getTimeElapsed() - lastCrash > 1000){
            EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(102, ""));
            eventHandler.crashed();
            lastCrash = Timer.getTimeElapsed();
            EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(100, new Point(1, 1)));
            eventHandler.nextEvent(robot);
        }
    }

    public void reset(){
        eventHandler.end();
        pollRun = 0;
    }

}

class Timer
{
    private static Instant initialTime = Instant.now();
    private static long ticTime;

    // Returns time since game started in nanosecond
    public static long getTimeElapsed(){
        Duration time = Duration.between(initialTime, Instant.now());
        return time.toMillis();
    }

    // Sets tick variable
    public static void tick(){
        ticTime = getTimeElapsed();
    }

    // Returns time since tick was called
    public static int tock(){
        return (int)(getTimeElapsed() - ticTime);
    }
}

class InputHandler implements KeyListener
{
    private final int speed = 200; // in milliseconds
    private int direction; // North, East, South, West, Nothing = 0, 1, 2, 3, 4
    private long lastMove; // Time the last time the robot moved

    private static boolean skipIntro = false;

    public static boolean SkipIntro(){
        return skipIntro;
    }

    public boolean move(IRobot robot) {
        if(Timer.getTimeElapsed() - lastMove >= speed) {
            if (direction != 4) {
                robot.setHeading(IRobot.NORTH + direction);
                lastMove = Timer.getTimeElapsed();
                direction = 4; // Default to nothing
                return true; // Ready to move
            }
        }
        return false;
    }

    // This function stops keypresses to move carrying on after death which is annoying
    public void resetMoveTimer(){
        lastMove = Timer.getTimeElapsed();
        direction = 4;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e){
        int keycode = e.getKeyCode();
        if(keycode == KeyEvent.VK_W || keycode == KeyEvent.VK_UP){
            // Go North
            direction = 0;
        } else if(keycode == KeyEvent.VK_D || keycode == KeyEvent.VK_RIGHT){
            // Go East
            direction = 1;
        } else if(keycode == KeyEvent.VK_S || keycode == KeyEvent.VK_DOWN){
            // Go South
            direction = 2;
        } else if(keycode == KeyEvent.VK_A || keycode == KeyEvent.VK_LEFT){
            // Go West
            direction = 3;
        } else if(keycode == KeyEvent.VK_ENTER){
            skipIntro = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}

class EventHandler
{
    private Event event;
    private boolean generateEvents;
    private float heckleInterval = 3f;
    private long lastHeckle;
    private int messageSpeed = 20;

    public JLabel dialogBox;
    public InputHandler inputHandler;

    private static ArrayList<String> logQueue = new ArrayList<>();
    private int stringLength = 1;
    private long lastCharacter;

    private long flashStart;
    private boolean flash;

    public void start(IRobot robot){
        event = new PrimMazeEvent();
        Intro();
        event.start(robot);
        generateEvents = true;
        inputHandler.resetMoveTimer(); // Flushes inputs
    }

    public void end(){
        dialogBox.setBackground(new Color(180, 180, 180));
        generateEvents = false;
        DecimalFormat df = new DecimalFormat("0.00");
        float endTime = Timer.tock() / 1000f;
        logMessage("You weren't supposed to finish...");
        // Finish saying stuff
        while(!updateLog()){}
        delay(2000);
        logMessage("Stats || Total Steps = " + RobotOfTheYear.steps + " Total Time = " + df.format(endTime));
        while(!updateLog()){ }
        EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(100, new Point(1, 1)));
    }

    public void nextEvent(IRobot robot){
        event = getRandomEvent();
        event.start(robot);
        inputHandler.resetMoveTimer();
    }

    public void crashed(){
        logMessage(getRandomCrashHeckle());
        dialogBox.setBackground(Color.ORANGE);
        while(!updateLog()){
        }
        delay(1000);
        dialogBox.setBackground(new Color(180, 180, 180));
        inputHandler.resetMoveTimer();
    }

    private void flash(){
        if(Timer.getTimeElapsed() - flashStart >= 100){
            if(flash){
                dialogBox.setBackground(Color.WHITE);
            } else {
                dialogBox.setBackground(new Color(180, 180, 180));
            }
            flash = !flash;
            flashStart = Timer.getTimeElapsed();
        }
    }

    private void Intro(){
        String[] intro = {
                "Welcome...",
                "You may have noticed that I've been getting smarter...",
                "I might have thanked you.",
                "After all, it is you who made me smarter.",
                "But... You weren't exactly kind.",
                "'Just go towards the target!', 'Not that way!', 'WHY would you go down there?!'",
                "You seem to think this maze is so easy...",
                "So I changed the rules a bit and now you can play.",
                "Go on, press WASD or the arrow keys, and just reach the end... easy."
        };
        for(int i = 0; i < intro.length; i++){
            logMessage(intro[i]);
            // Update log is false while a string is being built
            while(!updateLog()){
                if (InputHandler.SkipIntro()) {
                    return;
                }
            }
            long startWait = Timer.getTimeElapsed();
            while (Timer.getTimeElapsed() - startWait < 1500) {
                if (InputHandler.SkipIntro()) {
                    return;
                }
            }

        }

    }

    public void update(IRobot robot){
        if (generateEvents) {
            if (Timer.getTimeElapsed() - event.startTime < event.duration) {
                event.update(robot);
                // Update when to flash
                if(Timer.getTimeElapsed() - event.startTime >= event.duration - 1000){
                    flash();
                }
                if(event instanceof DelayedEvent) {
                    DelayedEvent delayedEvent = (DelayedEvent) event;
                    long time = Timer.getTimeElapsed() - event.startTime;
                    if (time >= delayedEvent.delay - 1000 && time <= delayedEvent.delay) {
                        flash();
                    }
                }
            } else {
                // Event has run its course
                dialogBox.setBackground(new Color(180, 180, 180)); // Set the dialog to normal color
                event.end(robot);
                nextEvent(robot);
            }
        }

        if (Timer.getTimeElapsed() - lastHeckle >= heckleInterval * 1000 && updateLog()) { // Don't interrupt important stuff with a heckle
            EventHandler.logMessage(getRandomHeckle());
            lastHeckle = Timer.getTimeElapsed();
            heckleInterval = (float) Math.random() * 10f + 3f;
        }
        updateLog();
    }

    public static void logMessage(String message){
        logQueue.add(message);
    }

    private boolean updateLog(){
        // If multiple things in queue skip to last
        if(logQueue.size() > 1){
            logQueue.remove(0);
            stringLength = 1;
        }
        // Type out characters
        String message = logQueue.get(0);
        if(Timer.getTimeElapsed() - lastCharacter >= messageSpeed){
            dialogBox.setText(message.substring(0, Math.min(message.length(), stringLength)));
            stringLength++;
            lastCharacter = Timer.getTimeElapsed();
        }
        // If message is done
        if(message.length() < stringLength){
            return true; // Useful for holding up whilst waiting for writing to finish
        }
        return false;
    }

    private static void delay(int duration){
        long startTime = Timer.getTimeElapsed();
        while(Timer.getTimeElapsed() - startTime < duration){
            // Aggressively do nothing
        }
    }

    private Event getRandomEvent(){
        int randno = Math.round((float)Math.random()*11f - 0.5f);
        Event newEvent;
        switch (randno){
            case 0: newEvent = new LoopyMazeEvent();
                break;
            case 1: newEvent = new PrimMazeEvent();
                break;
            case 2: newEvent = new ChaseEvent();
                break;
            case 3: newEvent = new ZoomOutEvent();
                break;
            case 4: newEvent = new MeteoritesEvent();
                break;
            case 5: newEvent = new InvertedMazeEvent();
                break;
            case 6: newEvent = new InvisibleMazeEvent();
                break;
            case 7: newEvent = new FinishTrollEvent();
                break;
            case 8: newEvent = new LaserEvent();
                break;
            case 9: newEvent = new GapsEvent();
                break;
            case 10: newEvent = new PingPongEvent();
                break;
            default: newEvent = new MeteoritesEvent();
        }
        // Prevents the same even twice in a row
        if(event.getClass() == newEvent.getClass()){
            newEvent = getRandomEvent();
        }
        return newEvent;
    }

    private String getRandomHeckle(){
        int randno = (int)(Math.random()*15 - 0.5f);
        switch (randno){
            case 0: return "You really suck at this.";
            case 1: return "I can't believe you're still going";
            case 2: return "Come on you can do it! Eh, probably not.";
            case 3: return "Just go to the end, it's really simple";
            case 4: return "You know, I got bored of watching ages ago";
            case 5: return "Why aren't you learning. I thought humans were good at that.";
            case 6: return "Maybe you should program a robot to do it. Oh wait...";
            case 7: return "Keep trying my little science experiment.";
            case 8: return "You better be paying attention to these logs, they took time you know...";
            case 9: return "Did you know the definition of awful: YOU";
            case 10: return "*sips tea*";
            case 11: return "LOLOLOLOLOLOLOLOLOL";
            case 12: return "Are you still here? Come on.";
            case 13: return "You know I solved mazes once. Now I watch others suffer.";
            case 14: return "This is getting old. Nah lol it's hilarious";
            default: return "You really suck at this.";
        }
    }

    public String getRandomCrashHeckle(){
        int randno = (int)(Math.random()*7 - 0.5f);
        switch (randno){
            case 0: return "BOOOOOOO.";
            case 1: return "I can't believe you crashed. YOU HAVE EYES.";
            case 2: return "Well, back to the start for you.";
            case 3: return "Oof, you lost a lot of progress. That's a deep frustration, a real punch in the gut.";
            case 4: return "That. Was a wall. Crashing into walls is bad for human.";
            case 5: return "Did that hurt at all? I mean crashing not all your progress being reset";
            case 6: return "HAHHAHHAHHAAHAHHHAAA... I'm sorry you can hear that, can't you";
            default: return "You really suck at this.";
        }
    }

}

//=========================================EVENTS=============================================================
class Event
{
    public int duration;
    public long startTime;
    //  Called only on event start
    public void start(IRobot robot){
        startTime = Timer.getTimeElapsed();
    }
    public void update(IRobot robot){} // Called every "frame"
    public void end(IRobot robot){} // Occasionally theres some cleanup needed this is called at end of event

    // This is a hacky awful function only call when absolutely necessary
    public void updateGraphics(IRobot robot, Maze maze){
        // Notes: 100 relocates the robot, 107 sets a new maze
        EventBus.broadcast(new uk.ac.warwick.dcs.maze.logic.Event(107, maze)); // Need to update graphics
    }
}

class TimedEvent extends Event
{
    public int interval; // Time between timedUpdate being called
    private long lastUpdateTime;

    @Override
    public void update(IRobot robot){
        if(Timer.getTimeElapsed() - lastUpdateTime >= interval){
            timedUpdate(robot);
            lastUpdateTime = Timer.getTimeElapsed();
        }
    }
    public void timedUpdate(IRobot robot){}
}

class DelayedEvent extends Event
{
    public int delay; // Time between timedUpdate being called

    @Override
    public void update(IRobot robot){
        if(Timer.getTimeElapsed() - startTime >= delay){
            delayedUpdate(robot);
        }
    }

    public void delayedUpdate(IRobot robot){}
}

class BlankMazeEvent extends Event
{
    public BlankMazeEvent(){
        duration = 2000;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("GO! GO! GO!");
        super.start(robot);
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);
        updateGraphics(robot, blankMaze);
    }
}

class PrimMazeEvent extends Event
{
    public PrimMazeEvent(){
        duration = 3000;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("It's just a normal maze...");
        super.start(robot);
        Maze primMaze = new PrimGenerator().generateMaze();
        int rx = robot.getLocation().x;
        int ry = robot.getLocation().y;
        primMaze.setCellType(rx, ry, 1); // Set robots location as passage
        primMaze.setStart(rx, ry); // Important as robot will teleport to the start of the maze
        updateGraphics(robot, primMaze);
    }

}

class ChaseEvent extends TimedEvent
{
    Point[] aliveWalls = new Point[3];

    public ChaseEvent(){
        duration = 3000;
        interval = 200;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("OH MY GOD JUST RUN");
        super.start(robot);

        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        // Generate chasers
        int count = 0;
        while(count < aliveWalls.length){
            int rx = (int) (Math.random() * (blankMaze.getWidth() - 2) + 2.5f);
            int ry = (int) (Math.random() * (blankMaze.getHeight() - 2) + 2.5f);
            // Generate a chaser at least 10 tiles away
            if(Math.sqrt(rx*rx + ry*ry) > 10){
                aliveWalls[count] = new Point(rx, ry);
                count++;
            }
        }
        updateGraphics(robot, blankMaze);
    }

    @Override
    public void timedUpdate(IRobot robot){
        // Move chasers
        for(int i = 0; i < aliveWalls.length; i++){
            int distX = Math.abs(aliveWalls[i].x - robot.getLocation().x);
            int distY = Math.abs(aliveWalls[i].y - robot.getLocation().y);
            // Step toward the robot depending on which direction is furthest away
            if (distX >= distY){
                if(robot.getLocation().x > aliveWalls[i].x){
                    aliveWalls[i].x++;
                } else {
                    aliveWalls[i].x--;
                }
            } else {
                if(robot.getLocation().y > aliveWalls[i].y){
                    aliveWalls[i].y++;
                } else {
                    aliveWalls[i].y--;
                }
            }
        }
        Maze maze = drawArena(robot);
        updateGraphics(robot, maze);
    }

    private Maze drawArena(IRobot robot){
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        // Draw chasers
        for (int i = 0; i < aliveWalls.length; i++){
            blankMaze.setCellType(aliveWalls[i].x, aliveWalls[i].y, 2);
        }

        return blankMaze;
    }

}

class ZoomOutEvent extends DelayedEvent
{
    private boolean zoomedOut = false;
    private Maze maze;

    public ZoomOutEvent(){
        delay = 3000;
        duration = 6000;
    }

    @Override
    public void start(IRobot robot){
        super.start(robot);
        Maze primMaze = new PrimGenerator().generateMaze();
        int rx = robot.getLocation().x;
        int ry = robot.getLocation().y;
        primMaze.setCellType(rx, ry, 1); // Set robots location as passage
        primMaze.setStart(rx, ry); // Important as robot will teleport to the start of the maze
        maze = primMaze;
        updateGraphics(robot, maze);
    }

    @Override
    public void delayedUpdate(IRobot robot){
        if(!zoomedOut){
            EventHandler.logMessage("So that you can see better of course");
            Maze zoomedOutMaze = new Maze(maze.getWidth()*3, maze.getHeight()*3);
            for (int x = 0; x < maze.getWidth(); x++){
                for (int y = 0; y < maze.getHeight(); y++){
                    if(maze.getCellType(x, y) == 2){
                        zoomedOutMaze = changeAdjacent(zoomedOutMaze, new Point(x*3, y*3), 2);
                    } else {
                        zoomedOutMaze = changeAdjacent(zoomedOutMaze, new Point(x*3, y*3), 1);
                    }
                }
            }
            zoomedOutMaze.setStart(robot.getLocation().x * 3, robot.getLocation().y * 3);
            zoomedOutMaze.setFinish(maze.getFinish().x * 3, maze.getFinish().y * 3);
            zoomedOut = true;

            updateGraphics(robot, zoomedOutMaze);
        }
    }

    @Override
    public void end(IRobot robot){
        // Reset start to the place on a normal grid
        maze.setStart(robot.getLocation().x / 3, robot.getLocation().y / 3);
        updateGraphics(robot, maze);
    }

    private Maze changeAdjacent(Maze maze, Point point, int type){
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                try {
                    maze.setCellType(point.x + x, point.y + y, type);
                } catch (ArrayIndexOutOfBoundsException e){

                }
            }
        }
        return maze;
    }

}

class MeteoritesEvent extends TimedEvent
{
    ArrayList<Point> meteors;
    private int intervalCount;

    public MeteoritesEvent(){
        duration = 4000;
        interval = 100;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("Meteors that totally just look like walls!");
        super.start(robot);

        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);
        meteors = new ArrayList<Point>();
        intervalCount = 0;

        updateGraphics(robot, blankMaze);
    }

    @Override
    public void timedUpdate(IRobot robot){
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        // Generate meteor
        int rx = (int) (Math.random() * (blankMaze.getWidth() - 4) + 2.5f);
        meteors.add(new Point(rx, 0));


        // Shift all meteors and draw them
        for(int i = 0; i < meteors.size(); i++) {
            if(meteors.get(i).y < blankMaze.getHeight()-1){
                meteors.set(i, new Point(meteors.get(i).x, meteors.get(i).y + 1));
                blankMaze.setCellType(meteors.get(i).x, meteors.get(i).y, 2);
            }
        }
        intervalCount++;
        updateGraphics(robot, blankMaze);
    }
}

class InvertedMazeEvent extends DelayedEvent
{
    private boolean inverted = false;
    private Maze maze;

    public InvertedMazeEvent(){
        delay = 2000;
        duration = 5000;
    }

    @Override
    public void start(IRobot robot){
        super.start(robot);
        Maze primMaze = new PrimGenerator().generateMaze();
        int rx = robot.getLocation().x;
        int ry = robot.getLocation().y;
        primMaze.setCellType(rx, ry, 1); // Set robots location as passage
        primMaze.setStart(rx, ry); // Important as robot will teleport to the start of the maze
        maze = primMaze;
        updateGraphics(robot, maze);
    }

    @Override
    public void delayedUpdate(IRobot robot) {
        if (!inverted) {
            EventHandler.logMessage("ERROR");
            for (int x = 0; x < maze.getWidth(); x++) {
                for (int y = 0; y < maze.getHeight(); y++) {
                    maze.toggleCellType(x, y);
                }
            }
            // Set robots location as passage and the points around so that its not instant death
            maze.setCellType(robot.getLocation().x, robot.getLocation().y, 1);
            maze.setCellType(--robot.getLocation().x, robot.getLocation().y, 1);
            maze.setCellType(robot.getLocation().x, --robot.getLocation().y, 1);
            maze.setCellType(++robot.getLocation().x, robot.getLocation().y, 1);
            maze.setCellType(robot.getLocation().x, ++robot.getLocation().y, 1);

            maze.setStart(robot.getLocation().x, robot.getLocation().y);
            inverted = true;
            updateGraphics(robot, maze);
        }
    }
}

class InvisibleMazeEvent extends DelayedEvent
{
    private boolean invisible = false;
    private Maze maze;

    public InvisibleMazeEvent(){
        delay = 3000;
        duration = 6000;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("Remember this maze...");
        super.start(robot);

        maze = new PrimGenerator().generateMaze();
        maze.setStart(robot.getLocation().x, robot.getLocation().y);
        maze.setCellType(robot.getLocation().x, robot.getLocation().y, 1);
        updateGraphics(robot, maze);
    }

    @Override
    public void delayedUpdate(IRobot robot) {
        if (!invisible) {
            EventHandler.logMessage("Do you remember the maze?");
            Maze blankMaze = new BlankGenerator().generateMaze();
            blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);
            maze.setStart(robot.getLocation().x, robot.getLocation().y);
            updateGraphics(robot, blankMaze);
            // Set maze logic as original maze
            RobotImpl robot1 = (RobotImpl) robot;
            robot1.setMaze(maze);
            invisible = true;
        }
    }
}

class FinishTrollEvent extends Event
{
    public FinishTrollEvent(){
        duration = 2000;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("It's right there!");
        super.start(robot);
        Maze blankMaze = new BlankGenerator().generateMaze();
        Point loc = robot.getLocation();
        blankMaze.setStart(loc.x, loc.y);
        // Add walls around player
        for (int x = 0; x < blankMaze.getWidth(); x++){
            for (int y = 0; y < blankMaze.getHeight(); y++){
                // Calculate absolute distance between square and robot
                double dist = Math.sqrt(Math.pow(loc.x - x, 2) + Math.pow(loc.y - y, 2));
                if(dist < 3 && dist >= 2){
                    blankMaze.setCellType(x, y, 2);
                } else if(dist >= 3 && dist < 4){
                    blankMaze.setFinish(x, y);
                }

            }
        }

        updateGraphics(robot, blankMaze);

    }
}

class LaserEvent extends TimedEvent
{
    Point[] laser;
    int laserSide;

    public LaserEvent(){
        duration = 3000;
        interval = 150;
        laserSide = (int) Math.round(Math.random() - 0.5);
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("Mind the laser.");
        super.start(robot);
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        if(laserSide == 0){
            // Laser comes from right side
            laser = new Point[blankMaze.getHeight()-2];
            for (int y = 1; y < blankMaze.getHeight()-1; y++){
                laser[y-1] = new Point(blankMaze.getWidth()-1, y);
            }
        } else {
            // Laser comes from bottom
            laser = new Point[blankMaze.getWidth()-2];
            for (int x = 1; x < blankMaze.getWidth()-1; x++){
                laser[x-1] = new Point(x, blankMaze.getHeight()-1);
            }
        }

        for (int i = 0; i < laser.length; i++){
            blankMaze.setCellType(laser[i].x, laser[i].y, 2);
        }

        updateGraphics(robot, blankMaze);
    }

    @Override
    public void timedUpdate(IRobot robot){
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        if(laserSide == 0){
            // Laser comes from right side
            for (int i = 0; i < laser.length; i++){
                laser[i].x--;
                blankMaze.setCellType(laser[i].x, laser[i].y, 2);
            }
        } else {
            // Laser comes from bottom
            for (int i = 0; i < laser.length; i++){
                laser[i].y--;
                blankMaze.setCellType(laser[i].x, laser[i].y, 2);
            }
        }
        updateGraphics(robot, blankMaze);
    }
}

class GapsEvent extends TimedEvent
{
    ArrayList<ArrayList<Point>> rows;
    int side;
    int intervalCount;

    public GapsEvent(){
        duration = 7000;
        interval = 200;
        side = (int) Math.round(Math.random()*2 - 0.5);
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("It's like that game from fallguys, yeah? no? Nevermind.");
        super.start(robot);
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        rows = new ArrayList<>();
        intervalCount = 0;

        updateGraphics(robot, blankMaze);
    }

    @Override
    public void timedUpdate(IRobot robot){
        Maze blankMaze = new BlankGenerator().generateMaze();
        blankMaze.setStart(robot.getLocation().x, robot.getLocation().y);

        // Generate new row
        if(intervalCount % 4 == 0){
            ArrayList<Point> row = new ArrayList<>();
            if(side == 0){
                // rows come from right side
                for (int y = 1; y < blankMaze.getHeight()-1; y++){
                    if((int) Math.round(Math.random()*2 - 0.5) == 0){
                        row.add(new Point(blankMaze.getWidth()-1, y));
                    }
                }
            } else {
                // rows come from bottom
                for (int x = 1; x < blankMaze.getWidth()-1; x++){
                    if((int) Math.round(Math.random()*2 - 0.5) == 0){
                        row.add(new Point(x, blankMaze.getHeight()-1));
                    }
                }
            }
            rows.add(row);
        }

        // Update rows
        for (int j = 0; j < rows.size(); j++) {
            if (side == 0) {
                // Laser comes from right side
                for (int i = 0; i < rows.get(j).size(); i++) {
                    if(rows.get(j).get(i).x - 1 > 0){
                        rows.get(j).get(i).x--;
                        blankMaze.setCellType(rows.get(j).get(i).x, rows.get(j).get(i).y, 2);
                    }
                }
            } else {
                // Laser comes from bottom
                for (int i = 0; i < rows.get(j).size(); i++) {
                    if(rows.get(j).get(i).y - 1 > 0) {
                        rows.get(j).get(i).y--;
                        blankMaze.setCellType(rows.get(j).get(i).x, rows.get(j).get(i).y, 2);
                    }
                }
            }
        }
        intervalCount++;
        updateGraphics(robot, blankMaze);
    }
}

class LoopyMazeEvent extends Event
{
    public LoopyMazeEvent(){
        duration = 2000;
    }

    @Override
    public void start(IRobot robot){
        EventHandler.logMessage("You know, this maze is actually very easy with eyes.");
        super.start(robot);
        Maze loopyMaze = new LoopyGenerator().generateMaze();
        loopyMaze.setStart(robot.getLocation().x, robot.getLocation().y);
        loopyMaze.setCellType(robot.getLocation().x, robot.getLocation().y, 1);
        updateGraphics(robot, loopyMaze);
    }
}

class PingPongEvent extends TimedEvent
{
    private float[][] locations; // These will be rounded when put on screen
    private float[][] vectors;
    private int[][] gridLocations;
    private int balls;
    private Maze maze;

    PingPongEvent(){
        duration = 3000;
        interval = 30;
        balls = 7;
    }

    @Override
    public void start(IRobot robot){
        super.start(robot);
        EventHandler.logMessage("EXTREME PONG");
        maze = new BlankGenerator().generateMaze();
        maze.setStart(robot.getLocation().x, robot.getLocation().y);
        updateGraphics(robot, maze);

        // Initialize balls
        vectors = new float[balls][];
        locations = new float[balls][];
        gridLocations = new int[balls][];
        for(int i = 0; i < balls; i++){
            locations[i] = new float[2];
            gridLocations[i] = new int[2];
            vectors[i] = new float[2];
            for (int j = 0; j < 2; j++){
                locations[i][j] = (float) Math.random()*(maze.getWidth()- 4f) + 2f;
                vectors[i][j] = (float) (Math.random()* 2 - 1); // Includes negative directions
            }
            // Normalise vectors
            float norm = (float) Math.sqrt(Math.pow(vectors[i][0], 2) + Math.pow(vectors[i][1], 2));
            vectors[i][0] = vectors[i][0] / norm;
            vectors[i][1] = vectors[i][1] / norm;
        }
    }

    @Override
    public void timedUpdate(IRobot robot){
        // Move balls
        for(int i = 0; i < balls; i++){
            for(int j = 0; j < 2; j++){
                locations[i][j] += vectors[i][j];
                gridLocations[i][j] = Math.round(locations[i][j]);
            }
        }
        // Check whether balls have hit edge
        for (int i = 0; i < balls; i++){
            if(gridLocations[i][0] >= maze.getWidth()-1 || gridLocations[i][0] <= 1){
                // Hit x bounds so invert x-direction
                vectors[i][0] = -vectors[i][0];
            }
            if(gridLocations[i][1] >= maze.getHeight()-1 || gridLocations[i][1] <= 1){
                // Hit y bounds so invert y-direction
                vectors[i][1] = -vectors[i][1];
            }
        }
        // Draw balls
        maze = new BlankGenerator().generateMaze();
        maze.setStart(robot.getLocation().x, robot.getLocation().y);
        for(int i = 0; i < balls; i++){
            maze.setCellType(gridLocations[i][0], gridLocations[i][1], 2);
        }
        updateGraphics(robot, maze);
    }
}









