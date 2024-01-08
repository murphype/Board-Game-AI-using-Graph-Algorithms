package scouts.AI.robot;

import java.util.Iterator;

import gamecore.algorithms.GraphAlgorithms;
import gamecore.datastructures.HashTable;
import gamecore.datastructures.LinkedList;
import gamecore.datastructures.graphs.AdjacencyListGraph;
import gamecore.datastructures.graphs.IGraph;
import gamecore.datastructures.matrices.Matrix2D;
import gamecore.datastructures.queues.Queue;
import gamecore.datastructures.tuples.Pair;
import gamecore.datastructures.tuples.Triple;
import gamecore.datastructures.vectors.Vector2d;
import gamecore.datastructures.vectors.Vector2i;
import scouts.AI.Artifact;
import scouts.AI.IRobot;
import scouts.AI.move.Direction;
import scouts.AI.move.Move;
import scouts.AI.move.VisionData;
import scouts.AI.robot.MyRobotOLD.MyEdge;
import scouts.AI.robot.MyRobotOLD.MyVertex;
import scouts.arena.Area;

public class MyRobot implements IRobot{
	
	
	//all of these are needed for vision
	protected int VertexCount = 0;
	protected AdjacencyListGraph <MyVertex, MyEdge> MyBoardInfo = new AdjacencyListGraph <MyVertex, MyEdge>(true); //total vision
	protected AdjacencyListGraph <Type2Vertex, MyEdge> currentInfo = new AdjacencyListGraph <Type2Vertex, MyEdge>(true); //vision from last update only
	protected IGraph<VisionData, Direction> currentInfoIG = null;
	protected Pair<Integer,Integer> currentLocation;
	protected Vector2i lastmove = new Vector2i(0,0);
	protected HashTable<Integer> playerIds = new HashTable<Integer>();
	protected VisionData lastCoordinateData; 
	protected int turnsofinactivity;
	protected Pair<Integer,Integer> AltarCoordinate = null;
	
	//needed for getmove
	protected int energy = 1000;
	protected Queue<Pair<Integer,Integer>> currentpath = new Queue<Pair<Integer,Integer>>();
	protected Direction exploreDirection = pickRandomDirection();
	protected int turnsUntilExploreChange = 0;
	protected boolean hitwall = false;
	
	//drone
	protected Pair<Integer,Integer> dronecoord = null;
	protected int dronealivetime = 0;
	
	
	//find altar
	protected int currentmode = 0;
	protected LinkedList<Area> quadrants_visited = new LinkedList<Area>();
	protected Pair<Integer,Integer> centerCoordinate = null;
	protected Pair<Integer,Integer> centerCoordinateReal = null;
	
	//battery
	protected boolean goingtobattery = false;
	protected Pair<Integer,Integer> batterycord = null;
	
	//find and capture artifact
	protected LinkedList<Pair<Integer,Integer>> IdolCoordinates = new LinkedList<Pair<Integer,Integer>>();
	protected LinkedList<Area> IdolTileEnvironments = new LinkedList<Area>();
	protected LinkedList<Artifact> ArtifactsCaptured = new LinkedList<Artifact>();
	protected Area currentRoamArea = null;
	protected Area startArea = null;
	protected Pair<Integer,Integer> startCoord = null;
	protected boolean madeitToRoamLocation = false;
	protected Direction currentdirection = pickRandomDirection();
	protected int turnstilldirectionChange = 0;
	protected boolean holdingartifact = false;
	protected Artifact artifactHolding = null;
	
	

	public class MyVertex
	{
		protected Pair<Integer,Integer> coordinate;
		protected VisionData importantTileInfo;
		protected int id;
		
		private MyVertex(Pair<Integer,Integer> coord, VisionData Info)
		{
			coordinate = coord;
			importantTileInfo = Info;
			id = VertexCount;
			VertexCount++;
		}
	}
	
	public class Type2Vertex
	{
		protected Pair<Integer,Integer> coordinate;
		protected VisionData importantTileInfo;
		protected int id;
		
		private Type2Vertex(Pair<Integer,Integer> coord, VisionData Info, int givenId)
		{
			coordinate = coord;
			importantTileInfo = Info;
			id = givenId;
		}
	}
	
	public class MyEdge
	{
		protected Pair<Integer,Integer> Startcoordinate;
		protected Pair<Integer,Integer> Endcoordinate;
		protected int cost;
		private MyEdge(Pair<Integer,Integer> start, Pair<Integer,Integer> end, int Cost)
		{
			Startcoordinate = start;
			Endcoordinate = end;
			cost = Cost;
		}
	}
	
	
	
	@Override
	public void Draw(Matrix2D cam) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean Initialized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void Update(long delta) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean Disposed() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public Move coordinateToMove(Pair<Integer,Integer> nextCoord)
	{
		
		if(nextCoord.Item1 == currentLocation.Item1 + 1 && nextCoord.Item2 == currentLocation.Item2)
		{
			lastmove = new Vector2i(1,0);
			System.out.println("Moving Up");
			return Move.CreateMovement(Direction.RIGHT);
		}
		else if(nextCoord.Item1 == currentLocation.Item1 - 1 && nextCoord.Item2 == currentLocation.Item2)
		{
			lastmove = new Vector2i(-1,0);
			System.out.println("Moving Up");
			return Move.CreateMovement(Direction.LEFT);
		}
		else if(nextCoord.Item1 == currentLocation.Item1 && nextCoord.Item2 == currentLocation.Item2 + 1)
		{
			lastmove = new Vector2i(0,1);
			System.out.println("Moving Up");
			return Move.CreateMovement(Direction.UP);
		}
		else if(nextCoord.Item1 == currentLocation.Item1 && nextCoord.Item2 == currentLocation.Item2 - 1)
		{
			lastmove = new Vector2i(0,-1);
			System.out.println("Moving Up");
			return Move.CreateMovement(Direction.DOWN);
		}
		else
		{
			System.out.println("ERROR: The nextcoordinate is more than one step away from the current coordinate");
			currentpath.Clear();
			throw new UnsupportedOperationException();
		}
	}
	
	
	public Move moveAlongPath()
	{
		if (currentpath.Count() < 0)
		{
			System.out.println("ERROR: We are trying to move along a path that doesn't exist");
			lastmove = new Vector2i(0,0);
			return Move.CreateNoOP();
		}
		
		Pair<Integer,Integer> nextCoord = currentpath.Dequeue();
		
		//obstacle check
		if(this.visionContainsCoordinate(nextCoord))//we might not be able to see our next move if we are on a mountain
		{
			if(this.getVertexfromCoord(nextCoord).importantTileInfo.IsWall() || this.getVertexfromCoord(nextCoord).importantTileInfo.HasRobot())
			{
				System.out.println("Attempted to run into something, so we didn't move");
				lastmove = new Vector2i(0,0);
				return Move.CreateNoOP();
			}
		}
		return coordinateToMove(nextCoord);
		
	}
	
	/*
	public boolean visionContainsCoord(Pair<Integer,Integer> coord)
	{
		if(this.getVertexfromCoord(coord) == null)
		{
			return false;
		}
		return true;
	}
	*/
	
	
	public Pair<Integer,Integer> DirectionToCoordinate(Direction dir)
	{
		if(dir == Direction.RIGHT)
		{
			return new Pair<Integer,Integer>(currentLocation.Item1 + 1,currentLocation.Item2);
		}
		else if(dir == Direction.LEFT)
		{
			return new Pair<Integer,Integer>(currentLocation.Item1 - 1,currentLocation.Item2);
		}
		else if(dir == Direction.UP)
		{
			return new Pair<Integer,Integer>(currentLocation.Item1,currentLocation.Item2 + 1);
		}
		else if(dir == Direction.DOWN)
		{
			return new Pair<Integer,Integer>(currentLocation.Item1,currentLocation.Item2 - 1);
		}
		return null;
	}
	
	public boolean isavailable(MyVertex v)
	{
		if(!v.importantTileInfo.IsWall() && !v.importantTileInfo.HasRobot())
		{
			return true;
		}
		return false;
	}
	
	
	public Move goToLocal(Pair<Integer,Integer> Coordinate)
	{
		if(Coordinate.Item1 > 0)
		{
			return Move.CreateMovement(Direction.RIGHT);
		}
		else if(Coordinate.Item1 < 0)
		{
			return Move.CreateMovement(Direction.LEFT);
		}
		else if(Coordinate.Item2 > 0)
		{
			return Move.CreateMovement(Direction.UP);
		}
		else if(Coordinate.Item2 < 0)
		{
			return Move.CreateMovement(Direction.DOWN);
		}
		return Move.CreateNoOP();
	}
	
	
	public Move goToCoordinateFast(Pair<Integer,Integer> Coordinate)
	{
		if(currentInfo.GetVertex(0).importantTileInfo.HasBattery)
		{
			lastmove = new Vector2i(0,0);
			return Move.CreateBatteryPickup();
		}
		
		
		System.out.println("Going to " + Coordinate);
		if(Coordinate.equals(currentLocation))
		{
			lastmove = new Vector2i(0,0);
			return Move.CreateNoOP();
		}
		
		
		Vector2i distanceToGo = new Vector2i(Coordinate.Item1 - currentLocation.Item1, Coordinate.Item2 - currentLocation.Item2);
		
		LinkedList <Direction> moves = new LinkedList <Direction>();
		Direction vert = null;
		Direction horiz = null;
		
		if(distanceToGo.X > 0)
		{
			horiz = Direction.RIGHT;
			moves.add(horiz);
		}
		else if(distanceToGo.X < 0)
		{
			horiz = Direction.LEFT;
			moves.add(horiz);
		}
		
		if(distanceToGo.Y > 0)
		{
			vert = Direction.UP;
			moves.add(vert);
		}
		else if(distanceToGo.Y < 0)
		{
			vert = Direction.DOWN;
			moves.add(vert);
		}
		
		Direction chosenDirection = null;
		if(moves.size() == 2)
		{
			if(this.visionContainsCoordinate(DirectionToCoordinate(vert)) && this.visionContainsCoordinate(DirectionToCoordinate(horiz)))
			{
				MyVertex vertVertex = this.getVertexfromCoord(DirectionToCoordinate(vert));
				MyVertex horizVertex = this.getVertexfromCoord(DirectionToCoordinate(horiz));
				if(isavailable(vertVertex) && isavailable(horizVertex))
				{
					if(vertVertex.importantTileInfo.Cost < horizVertex.importantTileInfo.Cost)
					{
						chosenDirection = vert;
					}
					else
					{
						chosenDirection = horiz;
					}
				}
				else if(isavailable(vertVertex))
				{
					chosenDirection = vert;
				}
				else if(isavailable(horizVertex))
				{
					chosenDirection = horiz;
				}
				else
				{
					chosenDirection = null;
				}
			}
			else if(this.visionContainsCoordinate(DirectionToCoordinate(vert)))
			{
				MyVertex vertVertex = this.getVertexfromCoord(DirectionToCoordinate(vert));
				if(isavailable(vertVertex))
				{
					chosenDirection = vert;
				}
				else
				{
					chosenDirection = horiz;
				}
			}
			else if(this.visionContainsCoordinate(DirectionToCoordinate(vert)))
			{
				MyVertex horizVertex = this.getVertexfromCoord(DirectionToCoordinate(horiz));
				if(isavailable(horizVertex))
				{
					chosenDirection = horiz;
				}
				else
				{
					chosenDirection = vert;
				}
			}
			else
			{
				int rando = (int) (Math.random() * 2);
				if(rando == 1)
				{
					chosenDirection = vert;
				}
				else
				{
					chosenDirection = horiz;
				}
			}
		}
		else if (moves.size() == 1)
		{	if(!this.visionContainsCoordinate(DirectionToCoordinate(moves.Front())))//we can't see in front of us
			{
				if(turnsofinactivity < 2)
				{
					chosenDirection = moves.Front();
				}
				else
				{
					chosenDirection = this.pickRandomPerpendicularDirection(moves.Front());
				}
			}
			else
			{
				MyVertex nextVertex = this.getVertexfromCoord(DirectionToCoordinate(moves.Front()));
				if(isavailable(nextVertex))
				{
					chosenDirection = moves.Front();
				}
				else
				{
					chosenDirection = this.pickRandomPerpendicularDirection(moves.Front());
				}
			}
			
		}//no else because moves.size should have 1 or two things
		
		if(chosenDirection == null)
		{
			lastmove = new Vector2i(0,0);
			return Move.CreateNoOP();
		}
		else
		{
			//System.out.println("CHOSEN DIRECTION IS " + chosenDirection);
			lastmove = this.directionToVector(chosenDirection);
			return Move.CreateMovement(chosenDirection);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	public Vector2i directionToVector(Direction mydir)
	{
		if(mydir == Direction.RIGHT)
		{
			return new Vector2i(1,0);
		}
		else if(mydir == Direction.LEFT)
		{
			return new Vector2i(-1,0);
		}
		else if(mydir == Direction.UP)
		{
			return new Vector2i(0,1);
		}
		else//down
		{
			return new Vector2i(0,-1);
		}
	}
	
	public Type2Vertex getLocalVertexFromCoord(Pair<Integer,Integer> Coord)
	{
		for(Type2Vertex vert : currentInfo.Vertices())
		{
			if(vert.coordinate.equals(Coord))
			{
				return vert;
			}
		}
		return null;
	}
	
	//pre: we are not on a mountain
	public LinkedList<Direction> getUnobstructedpath(Direction mydir)
	{
		if(currentInfo.GetVertex(0).importantTileInfo.IsMountain())
		{
			System.out.println("Get unobstructedpaths should't be called when on a mountain");
			throw new UnsupportedOperationException();
		}
		
		Pair<Integer,Integer> u = new Pair<Integer,Integer>(0,1);
		Pair<Integer,Integer> l = new Pair<Integer,Integer>(-1,0);
		Pair<Integer,Integer> d = new Pair<Integer,Integer>(0,-1);
		Pair<Integer,Integer> r = new Pair<Integer,Integer>(1,0);
		VisionData R = this.getLocalVertexFromCoord(r).importantTileInfo;
		VisionData L = this.getLocalVertexFromCoord(l).importantTileInfo;
		VisionData U = this.getLocalVertexFromCoord(u).importantTileInfo;
		VisionData D = this.getLocalVertexFromCoord(d).importantTileInfo;
		LinkedList<Direction> possibleDirection = new LinkedList<Direction>();
		if(mydir == Direction.RIGHT)
		{
			if(R.IsWall())
			{
				hitwall = true;
			}
			if(!R.IsWall() && !R.HasRobot())//free tile
			{
				possibleDirection.add(Direction.RIGHT);
			}
			VisionData cheaperdirect;
			VisionData expensivedirect;
			Direction ch;
			Direction ex;
			if(U.Cost < D.Cost)
			{
				cheaperdirect = U;
				ch = Direction.UP;
				expensivedirect = D;
				ex = Direction.DOWN;
			}
			else
			{
				cheaperdirect = D;
				ch = Direction.DOWN;
				expensivedirect = U;
				ex = Direction.UP;
			}
			if(!cheaperdirect.IsWall() && !cheaperdirect.HasRobot())//free tile
			{
				possibleDirection.add(ch);
			}
			if(!expensivedirect.IsWall() && !expensivedirect.HasRobot())//free tile
			{
				possibleDirection.add(ex);
			}
			
		}
		else if(mydir == Direction.LEFT)
		{
			if(L.IsWall())
			{
				hitwall = true;
			}
			if(!L.IsWall() && !L.HasRobot())//free tile
			{
				possibleDirection.add(Direction.LEFT);
			}
			VisionData cheaperdirect;
			VisionData expensivedirect;
			Direction ch;
			Direction ex;
			if(U.Cost < D.Cost)
			{
				cheaperdirect = U;
				ch = Direction.UP;
				expensivedirect = D;
				ex = Direction.DOWN;
			}
			else
			{
				cheaperdirect = D;
				ch = Direction.DOWN;
				expensivedirect = U;
				ex = Direction.UP;
			}
			if(!cheaperdirect.IsWall() && !cheaperdirect.HasRobot())//free tile
			{
				possibleDirection.add(ch);
			}
			if(!expensivedirect.IsWall() && !expensivedirect.HasRobot())//free tile
			{
				possibleDirection.add(ex);
			}
		}
		else if(mydir == Direction.UP)
		{
			if(U.IsWall())
			{
				hitwall = true;
			}
			if(!U.IsWall() && !U.HasRobot())//free tile
			{
				possibleDirection.add(Direction.UP);
			}
			VisionData cheaperdirect;
			VisionData expensivedirect;
			Direction ch;
			Direction ex;
			if(R.Cost < L.Cost)
			{
				cheaperdirect = R;
				ch = Direction.RIGHT;
				expensivedirect = L;
				ex = Direction.LEFT;
			}
			else
			{
				cheaperdirect = L;
				ch = Direction.LEFT;
				expensivedirect = R;
				ex = Direction.RIGHT;
			}
			if(!cheaperdirect.IsWall() && !cheaperdirect.HasRobot())//free tile
			{
				possibleDirection.add(ch);
			}
			if(!expensivedirect.IsWall() && !expensivedirect.HasRobot())//free tile
			{
				possibleDirection.add(ex);
			}
		}
		else if(mydir == Direction.DOWN)
		{
			if(D.IsWall())
			{
				hitwall = true;
			}
			if(!D.IsWall() && !D.HasRobot())//free tile
			{
				possibleDirection.add(Direction.DOWN);
			}
			VisionData cheaperdirect;
			VisionData expensivedirect;
			Direction ch;
			Direction ex;
			if(R.Cost < L.Cost)
			{
				cheaperdirect = R;
				ch = Direction.RIGHT;
				expensivedirect = L;
				ex = Direction.LEFT;
			}
			else
			{
				cheaperdirect = L;
				ch = Direction.LEFT;
				expensivedirect = R;
				ex = Direction.RIGHT;
			}
			if(!cheaperdirect.IsWall() && !cheaperdirect.HasRobot())//free tile
			{
				possibleDirection.add(ch);
			}
			if(!expensivedirect.IsWall() && !expensivedirect.HasRobot())//free tile
			{
				possibleDirection.add(ex);
			}
		}
		return possibleDirection;
	}
	
	
	
	
	
	
	
	//if we can't see we will move in the desired direction
	public Move moveEfficiently(Direction mydir, Area ar)
	{
		Pair<Integer,Integer> ur = new Pair<Integer,Integer>(1,1);
		Pair<Integer,Integer> u = new Pair<Integer,Integer>(0,1);
		Pair<Integer,Integer> ul = new Pair<Integer,Integer>(-1,1);
		Pair<Integer,Integer> l = new Pair<Integer,Integer>(-1,0);
		Pair<Integer,Integer> dl = new Pair<Integer,Integer>(-1,-1);
		Pair<Integer,Integer> d = new Pair<Integer,Integer>(0,-1);
		Pair<Integer,Integer> dr = new Pair<Integer,Integer>(1,-1);
		Pair<Integer,Integer> r = new Pair<Integer,Integer>(1,0);
		if(currentInfo.GetVertex(0).importantTileInfo.IsMountain())//current position local
		{
			if(turnsofinactivity == 0)
			{
				lastmove = directionToVector(mydir);
				return Move.CreateMovement(mydir);
			}
			else//we probably have an obstacle in from of us
			{
				Direction newdir = pickRandomPerpendicularDirection(mydir);
				if(ar != null)
				{
					return this.RandomMove();
				}
				lastmove = directionToVector(newdir);
				return Move.CreateMovement(newdir);
			}
		}
		else //we are not on a mountain
		{
			if(currentInfo.GetVertex(0).importantTileInfo.Environment != ar)
			{
				Area currentar = currentInfo.GetVertex(0).importantTileInfo.Environment;
				if(ar == Area.NE)
				{
					if(currentar == Area.NW)
					{
						lastmove = new Vector2i(1,0);
						Move.CreateMovement(Direction.RIGHT);
					}
					else if(currentar == Area.SE)
					{
						lastmove = new Vector2i(0,1);
						Move.CreateMovement(Direction.UP);
					}
				}
				else if(ar == Area.NW)
				{
					if(currentar == Area.NE)
					{
						lastmove = new Vector2i(-1,0);
						Move.CreateMovement(Direction.LEFT);
					}
					else if(currentar == Area.SW)
					{
						lastmove = new Vector2i(0,1);
						Move.CreateMovement(Direction.UP);
					}
				}
				else if(ar == Area.SE)
				{
					if(currentar == Area.SW)
					{
						lastmove = new Vector2i(-1,0);
						Move.CreateMovement(Direction.RIGHT);
					}
					else if(currentar == Area.NE)
					{
						lastmove = new Vector2i(0,-1);
						Move.CreateMovement(Direction.DOWN);
					}
				}
				else if(ar == Area.SW)
				{
					if(currentar == Area.NW)
					{
						lastmove = new Vector2i(0,-1);
						Move.CreateMovement(Direction.DOWN);
					}
					else if(currentar == Area.SE)
					{
						lastmove = new Vector2i(-1,0);
						Move.CreateMovement(Direction.LEFT);
					}
				}
			}
			
			
			
			
			LinkedList<Triple<Direction,Direction,Integer>> paths = new LinkedList<Triple<Direction,Direction,Integer>>();
			if(mydir == Direction.RIGHT)
			{
				Triple<Direction,Direction,Integer> rightP = 
						new Triple<Direction,Direction,Integer>(Direction.RIGHT,null,getLocalVertexFromCoord(r).importantTileInfo.Cost);
				if(ar == null || getLocalVertexFromCoord(r).importantTileInfo.Environment == ar)
				{
					paths.add(rightP);
				}
				
				if(getLocalVertexFromCoord(ur) != null)
				{
					Triple<Direction,Direction,Integer> rightupP = 
							new Triple<Direction,Direction,Integer>(Direction.RIGHT,Direction.UP,getLocalVertexFromCoord(r).importantTileInfo.Cost + getLocalVertexFromCoord(ur).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> uprightP = 
							new Triple<Direction,Direction,Integer>(Direction.UP,Direction.RIGHT,getLocalVertexFromCoord(u).importantTileInfo.Cost + getLocalVertexFromCoord(ur).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(r).importantTileInfo.Environment == ar)
					{
						paths.add(rightupP);
					}
					if(ar == null || getLocalVertexFromCoord(u).importantTileInfo.Environment == ar)
					{
						paths.add(uprightP);
					}
					
				}
				if(getLocalVertexFromCoord(dr) != null)
				{
					Triple<Direction,Direction,Integer> rightdownP = 
							new Triple<Direction,Direction,Integer>(Direction.RIGHT,Direction.DOWN,getLocalVertexFromCoord(r).importantTileInfo.Cost + getLocalVertexFromCoord(dr).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> downrightP = 
							new Triple<Direction,Direction,Integer>(Direction.DOWN,Direction.RIGHT,getLocalVertexFromCoord(d).importantTileInfo.Cost + getLocalVertexFromCoord(dr).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(r).importantTileInfo.Environment == ar)
					{
						paths.add(rightdownP);
					}
					if(ar == null || getLocalVertexFromCoord(d).importantTileInfo.Environment == ar)
					{
						paths.add(downrightP);
					}
				}
			}
			else if(mydir == Direction.LEFT)
			{
				Triple<Direction,Direction,Integer> leftP = 
						new Triple<Direction,Direction,Integer>(Direction.LEFT,null,getLocalVertexFromCoord(l).importantTileInfo.Cost);
				if(ar == null || getLocalVertexFromCoord(l).importantTileInfo.Environment == ar)
				{
					paths.add(leftP);
				}
				if(getLocalVertexFromCoord(ul) != null)
				{
					Triple<Direction,Direction,Integer> leftupP = 
							new Triple<Direction,Direction,Integer>(Direction.LEFT,Direction.UP,getLocalVertexFromCoord(l).importantTileInfo.Cost + getLocalVertexFromCoord(ul).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> upleftP = 
							new Triple<Direction,Direction,Integer>(Direction.UP,Direction.LEFT,getLocalVertexFromCoord(u).importantTileInfo.Cost + getLocalVertexFromCoord(ul).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(l).importantTileInfo.Environment == ar)
					{
						paths.add(leftupP);
					}
					if(ar == null || getLocalVertexFromCoord(u).importantTileInfo.Environment == ar)
					{
						paths.add(upleftP);
					}
					
				}
				if(getLocalVertexFromCoord(dl) != null)
				{
					Triple<Direction,Direction,Integer> leftdownP = 
							new Triple<Direction,Direction,Integer>(Direction.LEFT,Direction.DOWN,getLocalVertexFromCoord(l).importantTileInfo.Cost + getLocalVertexFromCoord(dl).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> downleftP = 
							new Triple<Direction,Direction,Integer>(Direction.DOWN,Direction.LEFT,getLocalVertexFromCoord(d).importantTileInfo.Cost + getLocalVertexFromCoord(dl).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(l).importantTileInfo.Environment == ar)
					{
						paths.add(leftdownP);
					}
					if(ar == null || getLocalVertexFromCoord(d).importantTileInfo.Environment == ar)
					{
						paths.add(downleftP);
					}
					
					
				}
			}
			else if(mydir == Direction.UP)
			{
				Triple<Direction,Direction,Integer> upP = 
						new Triple<Direction,Direction,Integer>(Direction.UP,null,getLocalVertexFromCoord(u).importantTileInfo.Cost);
				if(ar == null || getLocalVertexFromCoord(u).importantTileInfo.Environment == ar)
				{
					paths.add(upP);
				}
				if(getLocalVertexFromCoord(ul) != null)
				{
					Triple<Direction,Direction,Integer> leftupP = 
							new Triple<Direction,Direction,Integer>(Direction.LEFT,Direction.UP,getLocalVertexFromCoord(l).importantTileInfo.Cost + getLocalVertexFromCoord(ul).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> upleftP = 
							new Triple<Direction,Direction,Integer>(Direction.UP,Direction.LEFT,getLocalVertexFromCoord(u).importantTileInfo.Cost + getLocalVertexFromCoord(ul).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(l).importantTileInfo.Environment == ar)
					{
						paths.add(leftupP);
					}
					if(ar == null || getLocalVertexFromCoord(u).importantTileInfo.Environment == ar)
					{
						paths.add(upleftP);
					}
					
				}
				if(getLocalVertexFromCoord(ur) != null)
				{
					Triple<Direction,Direction,Integer> rightupP = 
							new Triple<Direction,Direction,Integer>(Direction.RIGHT,Direction.UP,getLocalVertexFromCoord(r).importantTileInfo.Cost + getLocalVertexFromCoord(ur).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> uprightP = 
							new Triple<Direction,Direction,Integer>(Direction.UP,Direction.RIGHT,getLocalVertexFromCoord(u).importantTileInfo.Cost + getLocalVertexFromCoord(ur).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(r).importantTileInfo.Environment == ar)
					{
						paths.add(rightupP);
					}
					if(ar == null || getLocalVertexFromCoord(u).importantTileInfo.Environment == ar)
					{
						paths.add(uprightP);
					}
					
					
				}
			}
			else if(mydir == Direction.DOWN)
			{
				Triple<Direction,Direction,Integer> downP = 
						new Triple<Direction,Direction,Integer>(Direction.DOWN,null,getLocalVertexFromCoord(d).importantTileInfo.Cost);
				if(ar == null || getLocalVertexFromCoord(d).importantTileInfo.Environment == ar)
				{
					paths.add(downP);
				}
				
				if(getLocalVertexFromCoord(dl) != null)
				{
					Triple<Direction,Direction,Integer> leftdownP = 
							new Triple<Direction,Direction,Integer>(Direction.LEFT,Direction.DOWN,getLocalVertexFromCoord(l).importantTileInfo.Cost + getLocalVertexFromCoord(dl).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> downleftP = 
							new Triple<Direction,Direction,Integer>(Direction.DOWN,Direction.LEFT,getLocalVertexFromCoord(d).importantTileInfo.Cost + getLocalVertexFromCoord(dl).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(l).importantTileInfo.Environment == ar)
					{
						paths.add(leftdownP);
					}
					if(ar == null || getLocalVertexFromCoord(d).importantTileInfo.Environment == ar)
					{
						paths.add(downleftP);
					}
					
					
				}
				if(getLocalVertexFromCoord(dr) != null)
				{
					Triple<Direction,Direction,Integer> rightdownP = 
							new Triple<Direction,Direction,Integer>(Direction.RIGHT,Direction.DOWN,getLocalVertexFromCoord(r).importantTileInfo.Cost + getLocalVertexFromCoord(dr).importantTileInfo.Cost);
					Triple<Direction,Direction,Integer> downrightP = 
							new Triple<Direction,Direction,Integer>(Direction.DOWN,Direction.RIGHT,getLocalVertexFromCoord(d).importantTileInfo.Cost + getLocalVertexFromCoord(dr).importantTileInfo.Cost);
					if(ar == null || getLocalVertexFromCoord(r).importantTileInfo.Environment == ar)
					{
						paths.add(rightdownP);
					}
					if(ar == null || getLocalVertexFromCoord(d).importantTileInfo.Environment == ar)
					{
						paths.add(downrightP);
					}
					
					
				}
			}
			
			
			Triple <Direction,Direction,Integer>bestpath = new Triple<Direction,Direction,Integer>(mydir,null,99999);
			int cheapestCost = 99999;
			for(Triple<Direction,Direction,Integer> path : paths)
			{
				if(path.Item3 < cheapestCost)
				{
					bestpath = path;
					cheapestCost = path.Item3;
				}
			}
			
			//checking collision
			LinkedList<Direction> unobstructedpaths = getUnobstructedpath(mydir);
			if(unobstructedpaths.contains(bestpath.Item1))
			{
				//System.out.println("Bestpath isn't blocked");
				lastmove = directionToVector(bestpath.Item1);
				return Move.CreateMovement(bestpath.Item1);
			}
			else if(unobstructedpaths.size() == 0) //we are blocked in 3 directions, players being annoying
			{
				//System.out.println("We are blocked in 3 direction, no move");
				lastmove = new Vector2i(0,0);
				return Move.CreateNoOP();
			}
			else
			{
				//System.out.println("Best path wasn't chosen since it was block, alternate move found");
				Direction dir = unobstructedpaths.get(0);
				lastmove = directionToVector(dir);
				return Move.CreateMovement(dir);
			}
		}
	}
	
	public Direction pickRandomDirection()
	{
		int rando = (int)(Math.random() * 4);
		if(rando == 0)
		{
			return Direction.RIGHT;
		}
		else if(rando == 1)
		{
			return Direction.LEFT;
		}
		if(rando == 2)
		{
			return Direction.DOWN;
		}
		else
		{
			return Direction.UP;
		}
	}
	
	public Direction pickRandomPerpendicularDirection(Direction mydirect)
	{
		if(mydirect.equals(Direction.UP))
		{
			int random = (int) (Math.random() * 2);
			if(random == 0)
			{
				return Direction.RIGHT;
			}
			else
			{
				return Direction.LEFT;
			}
		}
		else if(mydirect.equals(Direction.DOWN))
		{
			int random = (int) (Math.random() * 2);
			if(random == 0)
			{
				return Direction.RIGHT;
			}
			else
			{
				return Direction.LEFT;
			}
		}
		else if(mydirect.equals(Direction.RIGHT))
		{
			int random = (int) (Math.random() * 2);
			if(random == 0)
			{
				return Direction.UP;
			}
			else
			{
				return Direction.DOWN;
			}
		}
		else
		{
			int random = (int) (Math.random() * 2);
			if(random == 0)
			{
				return Direction.UP;
			}
			else
			{
				return Direction.DOWN;
			}
		}
	}
	
	
	
	
	
	public Move explore(Area a)//used when lost
	{
		if(currentInfo.GetVertex(0).importantTileInfo.HasBattery())
		{
			lastmove = new Vector2i(0,0);
			goingtobattery = false;
			batterycord = null;
			return Move.CreateBatteryPickup();
		}
		
		
		Pair<Integer,Integer> bat = this.batteryC(a);
		if(bat != null)
		{
			batterycord = bat;
			goingtobattery = true;
			return this.goToLocal(batterycord);
		}
		
		
		if(hitwall)
		{
			VisionData currentTileInfo = currentInfo.GetVertex(0).importantTileInfo;
			if(currentInfo.GetVertex(0).importantTileInfo.IsMountain())//we probably are stuck on a wall and can't see it
			{
				System.out.println("WE are stuck on a mountain");
				if(currentTileInfo.Environment == Area.NE)
				{
					exploreDirection = Direction.DOWN;
				}
				else if(currentTileInfo.Environment == Area.SE)
				{
					exploreDirection = Direction.LEFT;
				}
				else if(currentTileInfo.Environment == Area.SW)
				{
					exploreDirection = Direction.UP;
				}
				else if(currentTileInfo.Environment == Area.NW)
				{
					exploreDirection = Direction.RIGHT;
				}
				turnsUntilExploreChange = 8 + (int)(Math.random() * 9);
				hitwall = false;
			}
			else //we are not on a mountain so we can see at least our neighboring tiles
			{
				Pair<Integer,Integer> r = new Pair<Integer,Integer>(1,0);
				Pair<Integer,Integer> l = new Pair<Integer,Integer>(-1,0);
				Pair<Integer,Integer> u = new Pair<Integer,Integer>(0,1);
				Pair<Integer,Integer> d = new Pair<Integer,Integer>(0,-1);
				if(this.getLocalVertexFromCoord(r).importantTileInfo.IsWall())
				{
					exploreDirection = Direction.LEFT;
					turnsUntilExploreChange = 8 + (int)(Math.random() * 9);
				}
				else if(this.getLocalVertexFromCoord(l).importantTileInfo.IsWall())
				{
					exploreDirection = Direction.RIGHT;
					turnsUntilExploreChange = 8 + (int)(Math.random() * 9);
				}
				else if(this.getLocalVertexFromCoord(u).importantTileInfo.IsWall())
				{
					exploreDirection = Direction.DOWN;
					turnsUntilExploreChange = 8 + (int)(Math.random() * 9);
				}
				else if(this.getLocalVertexFromCoord(d).importantTileInfo.IsWall())
				{
					exploreDirection = Direction.UP;
					turnsUntilExploreChange = 8 + (int)(Math.random() * 9);
				}
				else //we are not next to a wall so we must have bumped into a player
				{
					exploreDirection = this.pickRandomPerpendicularDirection(exploreDirection);
					turnsUntilExploreChange = 4 + (int)(Math.random() * 5);
				}
				hitwall = false;
			}
		}
		else if (turnsUntilExploreChange == 0)
		{
			exploreDirection = this.pickRandomPerpendicularDirection(exploreDirection);
			turnsUntilExploreChange = 4 + (int)(Math.random() * 5);
		}
		
		turnsUntilExploreChange--;
		return moveEfficiently(exploreDirection, a);
	}
	
	
	
	
	
	
	
	
	public Move RandomMove()
	{
		System.out.println("picking random MOVE");
		int randomNumber = (int)(Math.random() * 4);
		if(randomNumber == 0)
		{
			lastmove = new Vector2i(0,1);
			System.out.println("Moving Up");
			return Move.CreateMovement(Direction.UP);
		}
		else if (randomNumber == 1)
		{
			lastmove = new Vector2i(0,-1);
			System.out.println("Moving Down");
			return Move.CreateMovement(Direction.DOWN);
		}
		else if (randomNumber == 2)
		{
			lastmove = new Vector2i(1,0);
			System.out.println("Moving Right");
			return Move.CreateMovement(Direction.RIGHT);
		}
		else if (randomNumber == 3)
		{
			lastmove = new Vector2i(-1,0);
			System.out.println("Moving Left");
			return Move.CreateMovement(Direction.LEFT);
		}
		
		throw new UnsupportedOperationException();	
	}
	
	
	public Move MoveInefficientQuadrant(Direction mydir)
	{
		if(turnsofinactivity > 0)
		{
			if(mydir == Direction.DOWN)//we are northeast
			{
				int rando = (int) (Math.random() * 2);
				if(rando == 0)
				{
					lastmove = directionToVector(Direction.DOWN);
					return Move.CreateMovement(Direction.DOWN);
				}
				else
				{
					lastmove = directionToVector(Direction.RIGHT);
					return Move.CreateMovement(Direction.RIGHT);
				}
			}
			else if(mydir == Direction.LEFT)//we are southeast
			{
				int rando = (int) (Math.random() * 2);
				if(rando == 0)
				{
					lastmove = directionToVector(Direction.LEFT);
					return Move.CreateMovement(Direction.LEFT);
				}
				else
				{
					lastmove = directionToVector(Direction.DOWN);
					return Move.CreateMovement(Direction.DOWN);
				}
			}
			else if(mydir == Direction.UP)//we are southwest
			{
				int rando = (int) (Math.random() * 2);
				if(rando == 0)
				{
					lastmove = directionToVector(Direction.UP);
					return Move.CreateMovement(Direction.UP);
				}
				else
				{
					lastmove = directionToVector(Direction.LEFT);
					return Move.CreateMovement(Direction.LEFT);
				}
			}
			else if(mydir == Direction.RIGHT)//we are northwest
			{
				int rando = (int) (Math.random() * 2);
				if(rando == 0)
				{
					lastmove = directionToVector(Direction.RIGHT);
					return Move.CreateMovement(Direction.RIGHT);
				}
				else
				{
					lastmove = directionToVector(Direction.UP);
					return Move.CreateMovement(Direction.UP);
				}
			}
		}
		else
		{
			lastmove = directionToVector(mydir);
			return Move.CreateMovement(mydir);
		}
		
		throw new UnsupportedOperationException();
	}
	
	public Move findCenter()
	{
		//updateIdolInfo();
		if(AltarCoordinate == null)
		{
			calculatealtar();
		}
		VisionData currentdata = currentInfo.GetVertex(0).importantTileInfo;
		if(!quadrants_visited.Contains(currentdata.Environment))
		{
			quadrants_visited.add(currentdata.Environment);
		}
		if(centerCoordinate == null && quadrants_visited.size() == 3)
		{
			centerCoordinate = currentLocation;
			dronecoord = currentLocation;
			dronealivetime = 1;
			System.out.println("adding drone");
			lastmove = new Vector2i(0,0);
			return Move.CreateDroneSummon(6, 62);
		}
		
		
		if(currentdata.Environment == Area.NE)
		{
			if(quadrants_visited.size() == 1)
			{
				return moveEfficiently(Direction.DOWN,null);
			}
			return MoveInefficientQuadrant(Direction.DOWN);
		}
		else if(currentdata.Environment == Area.SE)
		{
			if(quadrants_visited.size() == 1)
			{
				return moveEfficiently(Direction.LEFT,null);
			}
			return MoveInefficientQuadrant(Direction.LEFT);
		}
		else if(currentdata.Environment == Area.SW)
		{
			if(quadrants_visited.size() == 1)
			{
				return moveEfficiently(Direction.UP,null);
			}
			return MoveInefficientQuadrant(Direction.UP);
		}
		else if(currentdata.Environment == Area.NW)
		{
			if(quadrants_visited.size() == 1)
			{
				return moveEfficiently(Direction.RIGHT,null);
			}
			return MoveInefficientQuadrant(Direction.RIGHT);
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}
	
	
	
	public void calculatealtar()
	{
		//LinkedList<MyVertex> verts = new LinkedList<MyVertex>();
		int startid = this.getVertexfromCoord(currentLocation).id;
		GraphAlgorithms.BFS(MyBoardInfo,startid,(Graph,GraphId,Tree,TreeId) -> {
			MyVertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			if(currentVertex.importantTileInfo.IsAltar())
			{
				AltarCoordinate = currentcord;
			}
		});
	}
	
	public void updateIdolInfo()
	{
		//System.out.println("Updating Idol Info");
		//LinkedList<MyVertex> verts = new LinkedList<MyVertex>();
		int startid = this.getVertexfromCoord(currentLocation).id;
		GraphAlgorithms.BFS(MyBoardInfo,startid,(Graph,GraphId,Tree,TreeId) -> {
			MyVertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			Area currentarea = currentVertex.importantTileInfo.Environment;
			if(currentVertex.importantTileInfo.IsIdol())
			{
				if(!(IdolTileEnvironments.Contains(currentarea)))
				{
					IdolCoordinates.add(currentcord);
					IdolTileEnvironments.add(currentarea);
				}
				else//we already have the environment of the coord (updating existing)
				{
					System.out.println("Length of idolcoords " + IdolCoordinates.size());
					for(Pair <Integer,Integer> cord: IdolCoordinates)
					{
						System.out.println("coord: " + cord);
						MyVertex tempvert = this.getVertexfromCoord(cord);
						if(tempvert.importantTileInfo.Environment == currentarea)
						{
							System.out.println("We changed an existing idol coordinate");
							IdolCoordinates.Insert(currentcord, IdolCoordinates.indexOf(cord));
							IdolCoordinates.Remove(cord);
						}
					}
				}
				/*
				if(!(IdolCoordinates.Contains(currentcord)))//if it doesn't have that area
				{
					
				}
				else
				{
					System.out.println(currentcord + " is an idol coordinate that we already had");
				}
				*/
			}
		});
		
		System.out.println("Printing IDOLS that we have, We have: ");
		for(Area a : IdolTileEnvironments)
		{
			if(a == Area.NE)
			{
				System.out.print("NE, ");
			}
			else if(a == Area.SE)
			{
				System.out.print("SE, ");
			}
			else if(a == Area.NW)
			{
				System.out.print("NW, ");
			}
			else if(a == Area.SW)
			{
				System.out.println("SW, ");
			}
		}
		System.out.println();
	}
	
	
	
	public void calculatecenter()
	{
		LinkedList<MyVertex> verts = new LinkedList<MyVertex>();
		int startid = this.getVertexfromCoord(currentLocation).id;
		GraphAlgorithms.BFS(MyBoardInfo,startid,(Graph,GraphId,Tree,TreeId) -> {
			MyVertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			if(this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1 + 1,currentcord.Item2)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1 - 1,currentcord.Item2)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 + 1)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 - 1)))
			{
				if(this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1 + 1,currentcord.Item2)).importantTileInfo.Environment == Area.NE &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1 - 1,currentcord.Item2)).importantTileInfo.Environment == Area.NW &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 + 1)).importantTileInfo.Environment == Area.NE &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 - 1)).importantTileInfo.Environment == Area.SE)
				{
					verts.add(currentVertex);
				}
			}
		});
		if(verts.size() == 0)
		{
			return;
		}
		//now we can get the element because it exists
		centerCoordinateReal = verts.Front().coordinate;
		System.out.println("CENTER REAL IS " + centerCoordinateReal);
	}
	
	
	public Move findAltar()
	{
		updateIdolInfo();
		System.out.println("currentmode is " + currentmode);
		if(centerCoordinateReal == null)
		{
			calculatecenter();
		}
		if(AltarCoordinate == null)
		{
			calculatealtar();
		}
		
		if(centerCoordinateReal == null)
		{
			return explore(null);
		}
		
		if(currentmode == 0)
		{
			Pair<Integer,Integer> rightcenter = new Pair<Integer,Integer>(centerCoordinateReal.Item1 + 4, centerCoordinateReal.Item2);
			if(currentLocation.equals(rightcenter))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(rightcenter);
			}	
		}
		else if(currentmode == 1)
		{
			Pair<Integer,Integer> leftcenter = new Pair<Integer,Integer>(centerCoordinateReal.Item1 - 4, centerCoordinateReal.Item2);
			if(currentLocation.equals(leftcenter))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(leftcenter);
			}	
		}
		else if(currentmode == 2)
		{
			Pair<Integer,Integer> upcenter = new Pair<Integer,Integer>(centerCoordinateReal.Item1, centerCoordinateReal.Item2 + 4);
			if(currentLocation.equals(upcenter))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(upcenter);
			}	
		}
		else if(currentmode == 3)
		{
			Pair<Integer,Integer> downcenter = new Pair<Integer,Integer>(centerCoordinateReal.Item1, centerCoordinateReal.Item2 - 4);
			if(currentLocation.equals(downcenter))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(downcenter);
			}	
		}
		else if(currentmode == 4)
		{
			Pair<Integer,Integer> upright = new Pair<Integer,Integer>(centerCoordinateReal.Item1 + 4, centerCoordinateReal.Item2 + 4);
			if(currentLocation.equals(upright))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(upright);
			}	
		}
		else if(currentmode == 5)
		{
			Pair<Integer,Integer> upleft = new Pair<Integer,Integer>(centerCoordinateReal.Item1 - 4, centerCoordinateReal.Item2 + 4);
			if(currentLocation.equals(upleft))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(upleft);
			}	
		}
		else if(currentmode == 6)
		{
			Pair<Integer,Integer> downleft = new Pair<Integer,Integer>(centerCoordinateReal.Item1 - 4, centerCoordinateReal.Item2 - 4);
			if(currentLocation.equals(downleft))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(downleft);
			}	
		}
		else if(currentmode == 7)
		{
			Pair<Integer,Integer> downright = new Pair<Integer,Integer>(centerCoordinateReal.Item1 + 4, centerCoordinateReal.Item2 - 4);
			if(currentLocation.equals(downright))
			{
				currentmode++;
				lastmove = new Vector2i(0,0);
				dronealivetime = 1;
				return Move.CreateDroneSummon(5, 42);
			}
			else
			{
				return this.goToCoordinateFast(downright);
			}	
		}
		else
		{
			return explore(null);
		}
		
	}
	
	
	public int stepsrequired(Pair<Integer,Integer> coord)
	{
		if(coord == null)
		{
			return 99999;
		}
		int x = Math.abs(coord.Item1 - currentLocation.Item1);
		int y = Math.abs(coord.Item2 - currentLocation.Item2);
		return (x + y);
	}
	
	
	public Pair<Integer,Integer> getIdolCoord(Area ar)
	{
		for(Pair<Integer,Integer> cord : IdolCoordinates)
		{
			if(this.getVertexfromCoord(cord).importantTileInfo.Environment == ar)
			{
				return cord;
			}
		}
		System.out.println("couldnt find the matching coord");
		return null;
	}
	
	public Pair<Integer,Integer> findclosestIdol()
	{
		LinkedList <Pair<Integer,Integer>> IdolOptions = new LinkedList <Pair<Integer,Integer>>();
		if(IdolTileEnvironments.Contains(Area.NE) && !ArtifactsCaptured.Contains(Artifact.NE_IDOL))
		{
			System.out.println("Adding " + Artifact.NE_IDOL + " since we don't have it");
			IdolOptions.add(getIdolCoord(Area.NE));
		}
		else if(IdolTileEnvironments.Contains(Area.SE) && !ArtifactsCaptured.Contains(Artifact.SE_IDOL))
		{
			System.out.println("Adding " + Artifact.SE_IDOL + " since we don't have it");
			IdolOptions.add(getIdolCoord(Area.SE));
		}
		else if(IdolTileEnvironments.Contains(Area.NW) && !ArtifactsCaptured.Contains(Artifact.NW_IDOL))
		{
			System.out.println("Adding " + Artifact.NW_IDOL + " since we don't have it");
			IdolOptions.add(getIdolCoord(Area.NW));
		}
		else if(IdolTileEnvironments.Contains(Area.SW) && !ArtifactsCaptured.Contains(Artifact.SW_IDOL))
		{
			System.out.println("Adding " + Artifact.SW_IDOL + " since we don't have it");
			IdolOptions.add(getIdolCoord(Area.SW));
		}
		
		if(IdolOptions.size() == 0)
		{
			System.out.println("We should have run a contains check on IdolOptions");
			throw new UnsupportedOperationException();
		}
		else
		{
			int stepsrequiredbest = 99999;
			Pair<Integer,Integer> bestCoordinate = null;
			for(Pair<Integer,Integer> idolcoord : IdolOptions)
			{
				if(idolcoord == null)
				{
					//do nothing
				}
				else if(stepsrequired(idolcoord) < stepsrequiredbest)
				{
					bestCoordinate = idolcoord;
					stepsrequiredbest = stepsrequired(idolcoord);
				}
			}
			return bestCoordinate;
		}
	}
	
	
	
	public Pair<Integer,Integer> batteryC(Area ar)
	{
		LinkedList<Pair<Integer,Integer>> coords = new LinkedList<Pair<Integer,Integer>>();
		//int startid = this.getVertexfromCoord(currentLocation).id;
		GraphAlgorithms.BFS(currentInfo,0,(Graph,GraphId,Tree,TreeId) -> {
			Type2Vertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			if(ar == null)
			{
				if(currentVertex.importantTileInfo.HasBattery)
				{
					coords.add(currentcord);
					System.out.println("ADDED A BATTERYYYYY");
				}
			}
			else//we want a battery within a certain area
			{
				if(currentVertex.importantTileInfo.HasBattery && currentVertex.importantTileInfo.Environment.equals(ar))
				{
					coords.add(currentcord);
					System.out.println("ADDED A BATTERYYYYY");
				}
			}
		});
		for(Pair<Integer,Integer> coord : coords)
		{
			return coord;
		}
		return null;
	}
	
	
	
	public Pair<Integer,Integer> BatteryCoord(Area ar)
	{
		LinkedList<Pair<Integer,Integer>> coords = new LinkedList<Pair<Integer,Integer>>();
		int startid = this.getVertexfromCoord(currentLocation).id;
		GraphAlgorithms.BFS(MyBoardInfo,startid,(Graph,GraphId,Tree,TreeId) -> {
			MyVertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			if(ar == null)
			{
				if(currentVertex.importantTileInfo.HasBattery)
				{
					coords.add(currentcord);
					System.out.println("ADDED A BATTERYYYYY");
				}
			}
			else//we want a battery within a certain area
			{
				if(currentVertex.importantTileInfo.HasBattery && currentVertex.importantTileInfo.Environment.equals(ar))
				{
					coords.add(currentcord);
					System.out.println("ADDED A BATTERYYYYY");
				}
			}
		});
		for(Pair<Integer,Integer> coord : coords)
		{
			if(this.stepsrequired(coord) < 4)//dont go to a battery thats too far away (Waste of energy)
			{
				return coord;
			}
		}
		return null;
	}
	
	
	public void printregion(Area region)
	{
		if(region == Area.NE)
		{
			System.out.println("NE");
		}
		else if(region == Area.SE)
		{
			System.out.println("SE");
		}
		else if(region == Area.NW)
		{
			System.out.println("NW");
		}
		else if(region == Area.SW)
		{
			System.out.println("SW");
		}
	}
	
	public Move Roam(Area areatoroam)
	{
		//MyVertex currentstart = this.getVertexfromCoord(currentLocation);
		startArea = currentInfo.GetVertex(0).importantTileInfo.Environment;
		if(currentInfo.GetVertex(0).importantTileInfo.Environment != areatoroam)
		{
			madeitToRoamLocation = false;
		}
		else
		{
			madeitToRoamLocation = true;
		}
		/*else
		{
			madeitToRoamLocation = false;
		}
		*/
		if(!madeitToRoamLocation)
		{
			System.out.print("MOVING TO ROAM REGION : startregion is");
			this.printregion(startArea);
			System.out.print("ENDregion is ");
			this.printregion(areatoroam);
			if(areatoroam == Area.NE)
			{
				if(startArea == Area.NW )
				{
					lastmove = new Vector2i(1,0);
					return Move.CreateMovement(Direction.RIGHT);
				}
				else if(startArea == Area.SE )
				{
					lastmove = new Vector2i(0,1);
					return Move.CreateMovement(Direction.UP);
				}
				else //we are southwest
				{
					lastmove = new Vector2i(0,1);
					return Move.CreateMovement(Direction.UP);
				}
			}
			else if(areatoroam == Area.SE)
			{
				if(startArea == Area.SW )
				{
					lastmove = new Vector2i(1,0);
					return Move.CreateMovement(Direction.RIGHT);
				}
				else if(startArea == Area.NE )
				{
					lastmove = new Vector2i(0,-1);
					return Move.CreateMovement(Direction.DOWN);
				}
				else //we are northwest
				{
					lastmove = new Vector2i(0,-1);
					return Move.CreateMovement(Direction.DOWN);
				}
			}
			else if(areatoroam == Area.SW)
			{
				if(startArea == Area.SE )
				{
					lastmove = new Vector2i(-1,0);
					return Move.CreateMovement(Direction.LEFT);
				}
				else if(startArea == Area.NW )
				{
					lastmove = new Vector2i(0,-1);
					return Move.CreateMovement(Direction.DOWN);
				}
				else //we are northeast
				{
					lastmove = new Vector2i(0,-1);
					return Move.CreateMovement(Direction.DOWN);
				}
			}
			else if(areatoroam == Area.NW)
			{
				if(startArea == Area.NE )
				{
					lastmove = new Vector2i(-1,0);
					return Move.CreateMovement(Direction.LEFT);
				}
				else if(startArea == Area.SW )
				{
					lastmove = new Vector2i(0,1);
					return Move.CreateMovement(Direction.UP);
				}
				else //we are southeast
				{
					lastmove = new Vector2i(0,1);
					return Move.CreateMovement(Direction.UP);
				}
			}
		}
		else if(goingtobattery)
		{
			if(!(batterycord == null))
			{
				if(currentLocation.equals(batterycord))
				{
					lastmove = new Vector2i(0,0);
					goingtobattery = false;
					batterycord = null;
					return Move.CreateBatteryPickup();
				}
				//goingtobattery = true;
				return goToCoordinateFast(batterycord);
			}
			else
			{
				System.out.println("Battery Coord is null?");
			}
		}
		else
		{//create direction currentdirection
			//battery check:
			Pair<Integer,Integer> batteryCoord = BatteryCoord(areatoroam);
			if(!(batteryCoord == null))
			{
				batterycord = batteryCoord;
				goingtobattery = true;
				return goToCoordinateFast(batteryCoord);
			}
			
			System.out.println("There are no batteries in range");
			System.out.print("Roaming ");
			printregion(areatoroam);
			/*
			if(areatoroam == Area.NE)
			{
				if((DirectionToCoordinate(currentdirection)).Item1 < centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 < centerCoordinateReal.Item2)
				{
					currentdirection = pickOppositeRirection(currentdirection);
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
				
				else if(turnsofinactivity > 0 || turnstilldirectionChange == 0)//time to change direcions
				{
					currentdirection = this.pickRandomPerpendicularDirection(currentdirection);//changing the direction
					if((DirectionToCoordinate(currentdirection)).Item1 < centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 < centerCoordinateReal.Item2)
					{
						currentdirection = pickOppositeRirection(pickRandomPerpendicularDirection(currentdirection));//one these will not be out of bounds
					}
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
			}
			else if(areatoroam == Area.SE)
			{
				if((DirectionToCoordinate(currentdirection)).Item1 < centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 >= centerCoordinateReal.Item2)
				{
					currentdirection = pickOppositeRirection(currentdirection);
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
				
				else if(turnsofinactivity > 0 || turnstilldirectionChange == 0)//time to change direcions
				{
					currentdirection = this.pickRandomPerpendicularDirection(currentdirection);
					if((DirectionToCoordinate(currentdirection)).Item1 < centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 >= centerCoordinateReal.Item2)
					{
						currentdirection = pickOppositeRirection(pickRandomPerpendicularDirection(currentdirection));//one these will not be out of bounds
					}
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
			}
			else if(areatoroam == Area.NW)
			{
				if((DirectionToCoordinate(currentdirection)).Item1 >= centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 < centerCoordinateReal.Item2)
				{
					currentdirection = pickOppositeRirection(currentdirection);
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
				
				else if(turnsofinactivity > 0 || turnstilldirectionChange == 0)//time to change direcions
				{
					currentdirection = this.pickRandomPerpendicularDirection(currentdirection);
					if((DirectionToCoordinate(currentdirection)).Item1 >= centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 < centerCoordinateReal.Item2)
					{
						currentdirection = pickOppositeRirection(pickRandomPerpendicularDirection(currentdirection));//one these will not be out of bounds
					}
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
			}
			else if(areatoroam == Area.SW)
			{
				if((DirectionToCoordinate(currentdirection)).Item1 >= centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 >= centerCoordinateReal.Item2)
				{
					currentdirection = pickOppositeRirection(currentdirection);
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
				
				else if(turnsofinactivity > 0 || turnstilldirectionChange == 0)//time to change direcions
				{
					currentdirection = this.pickRandomPerpendicularDirection(currentdirection);
					if((DirectionToCoordinate(currentdirection)).Item1 >= centerCoordinateReal.Item1 || (DirectionToCoordinate(currentdirection)).Item2 >= centerCoordinateReal.Item2)
					{
						currentdirection = pickOppositeRirection(pickRandomPerpendicularDirection(currentdirection));//one these will not be out of bounds
					}
					turnstilldirectionChange = 4 + (int)(5 * (Math.random()));
				}
			}
			*/
		}
		return explore(areatoroam);
		/*
		turnstilldirectionChange--;
		lastmove = this.directionToVector(currentdirection);
		return Move.CreateMovement(currentdirection);
		*/
		//no else, this is a drop down, assume that we made it to the correct quadrant
		 
	}
	
	
	private Direction pickOppositeRirection(Direction dir) {
		if(dir == Direction.DOWN)
		{
			return Direction.UP;
		}
		else if(dir == Direction.UP)
		{
			return Direction.DOWN;
		}
		else if(dir == Direction.LEFT)
		{
			return Direction.RIGHT;
		}
		else if(dir == Direction.RIGHT)
		{
			return Direction.LEFT;
		}
		return null;
	}

	public Move searchIdolAndGetEnergy()
	{
		if(IdolTileEnvironments.size() == 4)
		{
			System.out.println("We found all idol spawners!");
			return explore(null);
		}
		else
		{
			if(currentRoamArea == null || IdolTileEnvironments.Contains(currentRoamArea))
			{
				//finding a new roaming area since we its null or already has an idol found
				LinkedList<Area> allIdolEnviros = new LinkedList<Area>();
				allIdolEnviros.add(Area.NE);
				allIdolEnviros.add(Area.SE);
				allIdolEnviros.add(Area.NW);
				allIdolEnviros.add(Area.SW);
				Area targetArea = null;
				for(Area ar : allIdolEnviros)
				{
					if(!IdolTileEnvironments.Contains(ar))
					{
						targetArea = ar;
						break;
					}
				}
				currentRoamArea = targetArea;
				madeitToRoamLocation = false;
				goingtobattery = false;
				System.out.println("changing roam areas");
				return Roam(currentRoamArea);//we are likely changing roam areas
			}
			else//we are now roaming this area
			{
				return Roam(currentRoamArea);
			}
		}
	}
	
	
	public boolean withinaltar()
	{
		Pair<Integer,Integer> altarc = AltarCoordinate;
		Pair<Integer,Integer> altaru = new Pair<Integer,Integer>(AltarCoordinate.Item1,AltarCoordinate.Item2 + 1);
		Pair<Integer,Integer> altard = new Pair<Integer,Integer>(AltarCoordinate.Item1,AltarCoordinate.Item2 - 1);
		Pair<Integer,Integer> altarl = new Pair<Integer,Integer>(AltarCoordinate.Item1 - 1,AltarCoordinate.Item2);
		Pair<Integer,Integer> altarr = new Pair<Integer,Integer>(AltarCoordinate.Item1 + 1,AltarCoordinate.Item2);
		if(currentLocation.equals(altarc) || 
			currentLocation.equals(altaru) || 
			currentLocation.equals(altard) || 
			currentLocation.equals(altarl) || 
			currentLocation.equals(altarr))
		{
			return true;
		}
		else
		{
			return false;
		}		
	}
	
	public Pair<Integer,Integer> getClosestCordToAltar()
	{
		Pair<Integer,Integer> altarc = AltarCoordinate;
		Pair<Integer,Integer> altaru = new Pair<Integer,Integer>(AltarCoordinate.Item1,AltarCoordinate.Item2 + 1);
		Pair<Integer,Integer> altard = new Pair<Integer,Integer>(AltarCoordinate.Item1,AltarCoordinate.Item2 - 1);
		Pair<Integer,Integer> altarl = new Pair<Integer,Integer>(AltarCoordinate.Item1 - 1,AltarCoordinate.Item2);
		Pair<Integer,Integer> altarr = new Pair<Integer,Integer>(AltarCoordinate.Item1 + 1,AltarCoordinate.Item2);
		
		LinkedList<Pair<Integer,Integer>> altarcoords = new LinkedList<Pair<Integer,Integer>>();
		altarcoords.add(altarc);
		altarcoords.add(altaru);
		altarcoords.add(altard);
		altarcoords.add(altarl);
		altarcoords.add(altarr);
		
		int bestcoordval = 99999;
		Pair<Integer,Integer>bestcoord = altarc;
		for(Pair<Integer,Integer> coord : altarcoords)
		{
			if(this.stepsrequired(coord) < bestcoordval)
			{
				bestcoord = coord;
				bestcoordval = stepsrequired(coord);
			}
		}
		return bestcoord;
	}
	
	public boolean newIdol()
	{
		Area a = currentInfo.GetVertex(0).importantTileInfo.Environment;
		if(a == Area.NE)
		{
			if(!ArtifactsCaptured.Contains(Artifact.NE_IDOL))
			{
				return true;
			}
		}
		if(a == Area.SE)
		{
			if(!ArtifactsCaptured.Contains(Artifact.SE_IDOL))
			{
				return true;
			}
		}
		if(a == Area.NW)
		{
			if(!ArtifactsCaptured.Contains(Artifact.NW_IDOL))
			{
				return true;
			}
		}
		if(a == Area.SW)
		{
			if(!ArtifactsCaptured.Contains(Artifact.SW_IDOL))
			{
				return true;
			}
		}
		return false;
	}
	
	public Move findIdolAndGetEnergy()
	{
		updateIdolInfo();
		boolean availableIdol = false;
		if(IdolTileEnvironments.Contains(Area.NE) && !ArtifactsCaptured.Contains(Artifact.NE_IDOL))
		{
			availableIdol = true;
		}
		else if(IdolTileEnvironments.Contains(Area.SE) && !ArtifactsCaptured.Contains(Artifact.SE_IDOL))
		{
			availableIdol = true;
		}
		else if(IdolTileEnvironments.Contains(Area.NW) && !ArtifactsCaptured.Contains(Artifact.NW_IDOL))
		{
			availableIdol = true;
		}
		else if(IdolTileEnvironments.Contains(Area.SW) && !ArtifactsCaptured.Contains(Artifact.SW_IDOL))
		{
			availableIdol = true;
		}
		
		if(availableIdol)
		{
			Pair<Integer,Integer> idolcoord = findclosestIdol();
			int distance = stepsrequired(idolcoord);
			int dronecostperturn = (2 * 13); // 3 * 13
			
			if(dronealivetime < 0)
			{
				if(energy > ((dronecostperturn * distance))) // + 800
				{
					if(withinaltar())//make drone
					{
						lastmove = new Vector2i(0,0);
						dronealivetime = 3 * distance;
						return Move.CreateDroneSummon(3, dronecostperturn * distance);
					}
					else//go to the drone stop
					{
						return this.goToCoordinateFast(this.getClosestCordToAltar());
					}
				}
				else
				{//we need more energy
					System.out.println("We didn't have enough energy to start an Idol hunt, energy needed: " + ((dronecostperturn * distance) + 800));
					return searchIdolAndGetEnergy();
				}
			}
			else //we should make our way to the idol or pick it up
			{
				System.out.println("Drone is up and moving to Idol");
				//we have a drone up and we can go to the artifact spawner
				if(currentInfo.GetVertex(0).importantTileInfo.IsIdol() && newIdol())
				{
					
					if(currentInfo.GetVertex(0).importantTileInfo.Environment.equals(Area.NE))
					{
						holdingartifact = true;
						artifactHolding = Artifact.NE_IDOL;
					}
					else if(currentInfo.GetVertex(0).importantTileInfo.Environment.equals(Area.SE))
					{
						holdingartifact = true;
						artifactHolding = Artifact.SE_IDOL;
					}
					else if(currentInfo.GetVertex(0).importantTileInfo.Environment.equals(Area.NW))
					{
						holdingartifact = true;
						artifactHolding = Artifact.NW_IDOL;
					}
					else if(currentInfo.GetVertex(0).importantTileInfo.Environment.equals(Area.SW))
					{
						holdingartifact = true;
						artifactHolding = Artifact.SW_IDOL;
					}
					else
					{
						System.out.println("CURRENLOCATION DIDN't ALIGN WITH IDOL");
					}
					holdingartifact = true;
					lastmove = new Vector2i(0,0);
					System.out.println("picking up artifact");
					return Move.CreateIdolPickup();
				}
				else if(!currentInfo.GetVertex(0).importantTileInfo.IsMountain())//we can see around us
				{
					Pair<Integer,Integer> right = new Pair<Integer,Integer>(1,0);
					Pair<Integer,Integer> left = new Pair<Integer,Integer>(-1,0);
					Pair<Integer,Integer> up = new Pair<Integer,Integer>(0,1);
					Pair<Integer,Integer> down = new Pair<Integer,Integer>(0,-1);
					if(this.getLocalVertexFromCoord(right).importantTileInfo.IsIdol())
					{
						return Move.CreateMovement(Direction.RIGHT);
					}
					else if(this.getLocalVertexFromCoord(left).importantTileInfo.IsIdol())
					{
						return Move.CreateMovement(Direction.LEFT);
					}
					else if(this.getLocalVertexFromCoord(up).importantTileInfo.IsIdol())
					{
						return Move.CreateMovement(Direction.UP);
					}
					else if(this.getLocalVertexFromCoord(down).importantTileInfo.IsIdol())
					{
						return Move.CreateMovement(Direction.DOWN);
					}
					if(turnsofinactivity < 2)
					{
						return goToCoordinateFast(idolcoord);
					}
					else
					{
						return this.RandomMove();
					}
					
				}
				else
				{
					if(turnsofinactivity > 3)
					{
						System.out.println("ERROR: We thing there is an idol here but there isn't");
					}
					return goToCoordinateFast(idolcoord);
				}
				
			}
		}
		else
		{
			return searchIdolAndGetEnergy();//there isn't an available idol
		}
		
	}
	
	
	
	public Move captureArtifact()
	{
		if(!currentInfo.GetVertex(0).importantTileInfo.IsMountain())
		{
			Pair<Integer,Integer> right = new Pair<Integer,Integer>(1,0);
			Pair<Integer,Integer> left = new Pair<Integer,Integer>(-1,0);
			Pair<Integer,Integer> up = new Pair<Integer,Integer>(0,1);
			Pair<Integer,Integer> down = new Pair<Integer,Integer>(0,-1);
			if(this.getLocalVertexFromCoord(right).importantTileInfo.IsAltar())
			{
				lastmove = new Vector2i(1,0);
				return Move.CreateMovement(Direction.RIGHT);
			}
			else if(this.getLocalVertexFromCoord(left).importantTileInfo.IsAltar())
			{
				lastmove = new Vector2i(-1,0);
				return Move.CreateMovement(Direction.LEFT);
			}
			else if(this.getLocalVertexFromCoord(up).importantTileInfo.IsAltar())
			{
				lastmove = new Vector2i(0,1);
				return Move.CreateMovement(Direction.UP);
			}
			else if(this.getLocalVertexFromCoord(down).importantTileInfo.IsAltar())
			{
				lastmove = new Vector2i(0,-1);
				return Move.CreateMovement(Direction.DOWN);
			}
		}
		
		if(currentInfo.GetVertex(0).importantTileInfo.IsAltar())
		{
			ArtifactsCaptured.Add(artifactHolding);
			Artifact temp = artifactHolding;
			artifactHolding = null;
			holdingartifact = false;
			lastmove = new Vector2i(0,0);
			
			if(ArtifactsCaptured.Contains(Artifact.NE_IDOL) && ArtifactsCaptured.Contains(Artifact.SE_IDOL) && 
					ArtifactsCaptured.Contains(Artifact.NW_IDOL) && ArtifactsCaptured.Contains(Artifact.SW_IDOL))
			{
				System.out.println("WE HAVE WON THE GAME");
				ArtifactsCaptured.Clear();
			}
			/*
			System.out.println("We just captured " + temp + ", All Artifacts captured: ");
			for(Artifact art : ArtifactsCaptured)
			{
				System.out.println(" " + art);
			}
			System.out.println();
			*/
			lastmove = new Vector2i(0,0);
			return Move.CreateIdolDrop(temp);
		}
		return this.goToCoordinateFast(AltarCoordinate);
	}
	
	@Override
	public Move GetNextAction() {
		
		this.fixcoordinatesystem();
		System.out.println("All Artifacts captured: ");
		for(Artifact art : ArtifactsCaptured)
		{
			System.out.println(" " + art);
		}
		System.out.println();
		//System.out.println("TURN OF INACTIVE: " + turnsofinactivity);
		//return RandomMove();//placeholder
		
		//System.out.println("getting next action");
		if(currentLocation == null)//if we are lost
		{
			return explore(null);
		}//now we can use currentLocation
		else if(centerCoordinate == null)
		{
			return findCenter();//we have an approx center location
		}
		else if(AltarCoordinate == null || centerCoordinateReal == null)
		{
			return findAltar();
		}
		else if (holdingartifact)
		{
			System.out.println("holding artifact: " + artifactHolding);
			return captureArtifact();
		}
		else
		{
			System.out.println("searching for idol");
			return findIdolAndGetEnergy();
		}
		
		
		//return this.goToCoordinateFast(centerCoordinateReal);
		//return Move.CreateNoOP();
		
		
	}

	@Override
	public void EnergyUpdate(int e) {
		System.out.println("Current energuy: " + e);
		energy = e;
		
	}
	
	
	//These are all the vision related functions

	public void printVision()
	{
		System.out.println("Global Printing Vertices:");
		for(Integer id : MyBoardInfo.VertexIDs())
		{
			System.out.println("VertexID: " + id + ", coordinate: " + MyBoardInfo.GetVertex(id).coordinate + " Cost of entering tile: " + MyBoardInfo.GetVertex(id).importantTileInfo.Cost);
		}
		/*
		System.out.println("Local Printing Vertices:");
		for(Integer id : currentInfo.VertexIDs())
		{
			System.out.println("VertexID: " + id + ", coordinate: " + currentInfo.GetVertex(id).coordinate + " Cost of entering tile: " + currentInfo.GetVertex(id).importantTileInfo.Cost);
		}
		*/
		System.out.println("Printing Edges:");
		for(Vector2i edge : MyBoardInfo.Edges())
		{
		
			System.out.println("Global: The cost of going from " + MyBoardInfo.GetVertex(edge.X).coordinate + " (ID " + MyBoardInfo.GetVertex(edge.X).id +") "
					+ " to " + MyBoardInfo.GetVertex(edge.Y).coordinate  + " is " + MyBoardInfo.GetEdge(edge.X, edge.Y).cost + " (ID " + MyBoardInfo.GetVertex(edge.Y).id +") ") ;
			
					
		}
		/*
		for(Vector2i edge : currentInfo.Edges())
		{
			System.out.println("Local: The cost of going from " + currentInfo.GetVertex(edge.X).coordinate + " (ID " + currentInfo.GetVertex(edge.X).id +") "
					+ " to " + currentInfo.GetVertex(edge.Y).coordinate  + " is " + currentInfo.GetEdge(edge.X, edge.Y).cost + " (ID " + currentInfo.GetVertex(edge.Y).id +") ") ;
		}
		*/
		
	}
	
	
	public boolean visionContainsCoordinate(Pair<Integer,Integer> coord)
	{
		for(MyVertex vert : MyBoardInfo.Vertices())
		{
			if(vert.coordinate.equals(coord))
			{
				return true;
			}
		}
		//System.out.println(coord + " is not inside the visions");
		return false;
	}
	
	public MyVertex getVertexfromCoord(Pair<Integer,Integer> coord)
	{
		for(MyVertex vert : MyBoardInfo.Vertices())
		{
			if(vert.coordinate.equals(coord))
			{
				return vert;
			}
		}
		return null;
	}
	
	
	public void copyGraph(IGraph<VisionData, Direction> robotvision)
	{
		int startid = 0;
		Pair<Integer,Integer> startcoord = new Pair<Integer,Integer>(0,0);
		Type2Vertex startVertex = new Type2Vertex(new Pair<Integer,Integer>(0,0),robotvision.GetVertex(0),0);
		currentInfo.AddVertex(startVertex);
		LinkedList <Pair<Pair<Integer,Integer>,Integer>> neighbordata = copyGraphHelper(robotvision,startcoord,startid);//radius1
		for(Pair<Pair<Integer,Integer>,Integer> data : neighbordata)
		{
			LinkedList <Pair<Pair<Integer,Integer>,Integer>> neighbordata2 = copyGraphHelper(robotvision,data.Item1,data.Item2); //radius2
			for(Pair<Pair<Integer,Integer>,Integer> data2 : neighbordata2)
			{
				copyGraphHelper(robotvision,data2.Item1,data2.Item2);
			}
		}
	}
	
	
	public LinkedList <Pair<Pair<Integer,Integer>,Integer>> copyGraphHelper(IGraph<VisionData, Direction> robotvision, Pair<Integer,Integer>startcoord, int startid)
	{
		LinkedList <Pair<Pair<Integer,Integer>,Integer>> neighbordata = new LinkedList <Pair<Pair<Integer,Integer>,Integer>>();
		//Type2Vertex startVertex = new Type2Vertex(new Pair<Integer,Integer>(0,0),robotvision.GetVertex(0),0);
		for(Integer id : robotvision.Neighbors(startid))
		{
			VisionData tempdata = robotvision.GetVertex(id);
			if(robotvision.GetEdge(startid, id) == Direction.RIGHT)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startcoord.Item1 + 1,startcoord.Item2);
				Type2Vertex tempVertex = new Type2Vertex(tempcoord,tempdata,id);
				currentInfo.AddVertex(tempVertex);
				MyEdge tempEdge1 = new MyEdge(startcoord,tempcoord,tempdata.Cost);
				MyEdge tempEdge2 = new MyEdge(tempcoord,startcoord,robotvision.GetVertex(startid).Cost);
				currentInfo.AddEdge(startid,id,tempEdge1);
				currentInfo.AddEdge(id, startid, tempEdge2);
				neighbordata.add(new Pair<Pair<Integer,Integer>,Integer>(tempcoord,id));
			}
			else if(robotvision.GetEdge(startid, id) == Direction.LEFT)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startcoord.Item1 - 1,startcoord.Item2);
				Type2Vertex tempVertex = new Type2Vertex(tempcoord,tempdata,id);
				currentInfo.AddVertex(tempVertex);
				MyEdge tempEdge1 = new MyEdge(startcoord,tempcoord,tempdata.Cost);
				MyEdge tempEdge2 = new MyEdge(tempcoord,startcoord,robotvision.GetVertex(startid).Cost);
				currentInfo.AddEdge(startid,id,tempEdge1);
				currentInfo.AddEdge(id, startid, tempEdge2);
				neighbordata.add(new Pair<Pair<Integer,Integer>,Integer>(tempcoord,id));
			}
			else if(robotvision.GetEdge(startid, id) == Direction.UP)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startcoord.Item1,startcoord.Item2 + 1);
				Type2Vertex tempVertex = new Type2Vertex(tempcoord,tempdata,id);
				currentInfo.AddVertex(tempVertex);
				MyEdge tempEdge1 = new MyEdge(startcoord,tempcoord,tempdata.Cost);
				MyEdge tempEdge2 = new MyEdge(tempcoord,startcoord,robotvision.GetVertex(startid).Cost);
				currentInfo.AddEdge(startid,id,tempEdge1);
				currentInfo.AddEdge(id, startid, tempEdge2);
				neighbordata.add(new Pair<Pair<Integer,Integer>,Integer>(tempcoord,id));
			}
			else if(robotvision.GetEdge(startid, id) == Direction.DOWN)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startcoord.Item1,startcoord.Item2 - 1);
				Type2Vertex tempVertex = new Type2Vertex(tempcoord,tempdata,id);
				currentInfo.AddVertex(tempVertex);
				MyEdge tempEdge1 = new MyEdge(startcoord,tempcoord,tempdata.Cost);
				MyEdge tempEdge2 = new MyEdge(tempcoord,startcoord,robotvision.GetVertex(startid).Cost);
				currentInfo.AddEdge(startid,id,tempEdge1);
				currentInfo.AddEdge(id, startid, tempEdge2);
				neighbordata.add(new Pair<Pair<Integer,Integer>,Integer>(tempcoord,id));
			}
		}
		return neighbordata;
	}
	
	
	
	
	public LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> addNeighborVertsAndEdges(IGraph<VisionData, Direction> robotvision, int localstartId, Pair<Integer,Integer> startCoord)
	{
		LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> neighborVertexInfo = new LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>>();
		//System.out.println("LocalstartId is " + localstartId);
		//System.out.println("The vertex from the id is " + robotvision.GetVertex(localstartId));
		
		for(int neighborVertexId : robotvision.Neighbors(localstartId))//get the neighbors of the center
		{
			//System.out.println("NeighborVertexId is " + neighborVertexId);
			Direction vertexDirect = robotvision.GetEdge(localstartId, neighborVertexId);
			VisionData tempData = robotvision.GetVertex(neighborVertexId);
			int globalstartID = this.getVertexfromCoord(startCoord).id;
			if(vertexDirect == Direction.UP)
			{
				
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startCoord.Item1,startCoord.Item2 + 1);
				
				if(!this.visionContainsCoordinate(tempcoord))
				{	
					MyVertex tempVertex = new MyVertex(tempcoord,tempData);
					MyBoardInfo.AddVertex(tempVertex);
					MyEdge tempEdge1 = new MyEdge(startCoord,tempcoord,tempData.Cost);
					MyEdge tempEdge2 = new MyEdge(tempcoord,startCoord, robotvision.GetVertex(localstartId).Cost);
					MyBoardInfo.AddEdge(globalstartID,tempVertex.id,tempEdge1);
					MyBoardInfo.AddEdge(tempVertex.id,globalstartID,tempEdge2);
				}
				else//updating information because this vertex exists
				{
					getVertexfromCoord(tempcoord).importantTileInfo = robotvision.GetVertex(neighborVertexId);
					//System.out.println("Updating " + tempcoord);
				}
				Triple <Integer,Integer,Pair<Integer,Integer>> tempTriple = new Triple<Integer,Integer,Pair<Integer,Integer>> (neighborVertexId,null, tempcoord);
				neighborVertexInfo.add(tempTriple);
				
				
			}
			else if(vertexDirect == Direction.DOWN)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startCoord.Item1,startCoord.Item2 - 1);
				
				if(!this.visionContainsCoordinate(tempcoord))
				{
					MyVertex tempVertex = new MyVertex(tempcoord,tempData);
					MyBoardInfo.AddVertex(tempVertex);
					MyEdge tempEdge1 = new MyEdge(startCoord,tempcoord,tempData.Cost);
					MyEdge tempEdge2 = new MyEdge(tempcoord,startCoord,robotvision.GetVertex(localstartId).Cost);
					MyBoardInfo.AddEdge(globalstartID,tempVertex.id,tempEdge1);
					MyBoardInfo.AddEdge(tempVertex.id,globalstartID,tempEdge2);
				}
				else//updating information because this vertex exists
				{
					getVertexfromCoord(tempcoord).importantTileInfo = robotvision.GetVertex(neighborVertexId);
					//System.out.println("Updating " + tempcoord);
				}
				Triple <Integer,Integer,Pair<Integer,Integer>> tempTriple = new Triple<Integer,Integer,Pair<Integer,Integer>> (neighborVertexId, null, tempcoord);
				neighborVertexInfo.add(tempTriple);
			}
			else if(vertexDirect == Direction.RIGHT)
			{
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startCoord.Item1 + 1,startCoord.Item2);
				if(!this.visionContainsCoordinate(tempcoord))
				{
					MyVertex tempVertex = new MyVertex(tempcoord,tempData);
					MyBoardInfo.AddVertex(tempVertex);
					MyEdge tempEdge1 = new MyEdge(startCoord,tempcoord,tempData.Cost);
					MyEdge tempEdge2 = new MyEdge(tempcoord,startCoord,robotvision.GetVertex(localstartId).Cost);
					MyBoardInfo.AddEdge(globalstartID,tempVertex.id,tempEdge1);
					MyBoardInfo.AddEdge(tempVertex.id,globalstartID,tempEdge2);
				}
				else//updating information because this vertex exists
				{
					getVertexfromCoord(tempcoord).importantTileInfo = robotvision.GetVertex(neighborVertexId);
					//System.out.println("Updating " + tempcoord);
				}
				Triple <Integer,Integer,Pair<Integer,Integer>> tempTriple = new Triple<Integer,Integer,Pair<Integer,Integer>> (neighborVertexId, null, tempcoord);
				neighborVertexInfo.add(tempTriple);
			}
			else if(vertexDirect == Direction.LEFT)
			{
				
				Pair<Integer,Integer> tempcoord = new Pair<Integer,Integer>(startCoord.Item1 - 1,startCoord.Item2);
				
				if(!this.visionContainsCoordinate(tempcoord))
				{
					MyVertex tempVertex = new MyVertex(tempcoord,tempData);
					MyBoardInfo.AddVertex(tempVertex);
					MyEdge tempEdge1 = new MyEdge(startCoord,tempcoord,tempData.Cost);
					MyEdge tempEdge2 = new MyEdge(tempcoord,startCoord,robotvision.GetVertex(localstartId).Cost);
					MyBoardInfo.AddEdge(globalstartID,tempVertex.id,tempEdge1);
					MyBoardInfo.AddEdge(tempVertex.id,globalstartID,tempEdge2);
				}
				else//updating information because this vertex exists
				{
					getVertexfromCoord(tempcoord).importantTileInfo = robotvision.GetVertex(neighborVertexId);
					//System.out.println("Updating " + tempcoord);
				}
				Triple <Integer,Integer,Pair<Integer,Integer>> tempTriple = new Triple<Integer,Integer,Pair<Integer,Integer>> (neighborVertexId,null, tempcoord);
				neighborVertexInfo.add(tempTriple);	
			}
			else
			{
				throw new UnsupportedOperationException();
			}
		}
		return neighborVertexInfo;
	}
	
	
	//currentlocation is updated here
	@Override
	public void VisionUpdate(Iterable<IGraph<VisionData, Direction>> vision) {
		
		//System.out.println("Updataing vision");
		
		Iterator<IGraph<VisionData, Direction>> itr = vision.iterator();
		IGraph<VisionData, Direction> robotvision = (IGraph<VisionData, Direction>) itr.next();
		currentInfoIG = robotvision;
		IGraph<VisionData, Direction> dronevision = null;
		if(itr.hasNext())
		{
			dronevision = itr.next();
			if(dronevision.VertexCount() < 13)
			{
				dronevision = null;//it wasn't actually a drone
			}
			else
			{
				//System.out.println("WE HAVE A DRONE?");
			}
		}
		
		
		VisionData givenCenterData = robotvision.GetVertex(0);//the center of the vision graph
		VisionData givenDroneCenterData = null;
		if(dronevision != null)
		{
			givenDroneCenterData = dronevision.GetVertex(0);
		}
		
		currentInfo = new AdjacencyListGraph<Type2Vertex, MyEdge>(true);
		this.copyGraph(robotvision);
		
		
		if(MyBoardInfo.VertexCount() == 0) //we have no information and this is the first time receiving it
		{//we will now set this position as our reference position
			lastCoordinateData = givenCenterData;
			currentLocation = new Pair<Integer,Integer>(0,0);
			turnsofinactivity = 0;
			
		}
		else
		{
			if(currentLocation == null)
			{
				//we moved
				if(!(TilesAreTheSameUncertain(lastCoordinateData, givenCenterData)))
				{
					lastCoordinateData = givenCenterData;
					turnsofinactivity = 0;
				}
				else
				{
					System.out.println("WE DID NOT MOVE");
					lastmove = new Vector2i(0,0);
					turnsofinactivity++;
				}
				
				Pair<Integer,Integer> potentialCenterCoord = this.whatIsOurCoord();
				if(potentialCenterCoord == null)
				{
					System.out.println("WE ARE LOST :(");
					if(dronealivetime > 0)
					{
						dronealivetime--;
					}
					return;
				}
				else
				{
					System.out.println("WE FOUND WHERE WE ARE!");
					lastmove = new Vector2i(0,0);
					currentLocation = potentialCenterCoord;
				}
			}
			//checking to see if the center tile are the same (in this case we have moved)
		    else if(!(TilesAreTheSameUncertain(lastCoordinateData,givenCenterData)))
			{
				currentLocation = new Pair<Integer, Integer>(currentLocation.Item1 + lastmove.X, currentLocation.Item2 + lastmove.Y);
				lastCoordinateData = givenCenterData;
				turnsofinactivity = 0;
			}
			else
			{
				System.out.println("WE DID NOT MOVE");
				lastmove = new Vector2i(0,0);
				turnsofinactivity++;
			}
		}
		System.out.println("Current Location: " + currentLocation);
		
		if(!this.visionContainsCoordinate(currentLocation))//add the center of the new vision graph if its new
		{
			MyVertex centerVertex = new MyVertex(currentLocation,givenCenterData);
			MyBoardInfo.AddVertex(centerVertex);
		}
		else
		{
			//System.out.println("Updating " + currentLocation);
			getVertexfromCoord(currentLocation).importantTileInfo = givenCenterData;//update the center if we already visited it
			if(givenDroneCenterData != null)
			{
				getVertexfromCoord(dronecoord).importantTileInfo = givenDroneCenterData;
			}
			
		}
		
		//adding robot info to main vision
		LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> neighborVertexDataRadius1 = this.addNeighborVertsAndEdges(robotvision,0,currentLocation);//this adds verteces and edges radius1
		for(Triple<Integer,Integer,Pair<Integer,Integer>> dataTripleRad1 : neighborVertexDataRadius1)
		{
			LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> neighborVertexDataRadius2 = this.addNeighborVertsAndEdges(robotvision, dataTripleRad1.Item1, dataTripleRad1.Item3);//this adds verteces and edges radius2
			for(Triple<Integer,Integer,Pair<Integer,Integer>> dataTripleRad2 : neighborVertexDataRadius2)
			{
				this.addNeighborVertsAndEdges(robotvision, dataTripleRad2.Item1, dataTripleRad2.Item3);//this adds verteces and edges radius 3
			}
		}
		
		
		
		
		//adding drone info if it is present to main vision
		if(givenDroneCenterData != null)
		{
			/*
			System.out.println("drone verteces");
			for(Integer id : dronevision.VertexIDs())
			{
				System.out.print(" " + id);
			}
			System.out.println();
			*/
			
			//System.out.println("WE HAVE A DRONE At Coord: " + dronecoord);
			LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad1 = this.addNeighborVertsAndEdges(dronevision,0,dronecoord);//this adds verteces and edges radius1
			for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR1 : DroneRad1)
			{
				LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad2 = this.addNeighborVertsAndEdges(dronevision, DTR1.Item1, DTR1.Item3);//this adds verteces and edges radius2
				for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR2: DroneRad2)
				{
					LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad3 = this.addNeighborVertsAndEdges(dronevision, DTR2.Item1, DTR2.Item3);//this adds verteces and edges radius3
					
					for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR3 : DroneRad3)
					{
						LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad4 = this.addNeighborVertsAndEdges(dronevision, DTR3.Item1, DTR3.Item3);//this adds verteces and edges radius4
						for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR4: DroneRad4)
						{
							LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad5 = this.addNeighborVertsAndEdges(dronevision, DTR4.Item1, DTR4.Item3);//this adds verteces and edges radius5
							for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR5: DroneRad5)
							{
								LinkedList<Triple<Integer,Integer,Pair<Integer,Integer>>> DroneRad6 = this.addNeighborVertsAndEdges(dronevision, DTR5.Item1, DTR5.Item3);//this adds verteces and edges radius6
								for(Triple<Integer,Integer,Pair<Integer,Integer>> DTR6: DroneRad6)
								{
									this.addNeighborVertsAndEdges(dronevision, DTR6.Item1, DTR6.Item3);//this adds verteces and edges radius 7
								}
							}
						}
					}
				}
			}
		}
		
		if(dronealivetime > 0)
		{
			dronealivetime--;
		}
		//this.printVision();
		System.out.println("Altar coordinate " + AltarCoordinate);
		//System.out.println("Center coordinate approx " + centerCoordinate);
		//System.out.println("Center coordinate real " + centerCoordinateReal);
		this.calculatealtar();
		this.updateIdolInfo();
		return;
	}

	@Override
	public void ReportSuccess(boolean success) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean StruggleForArtifact(Artifact a, int cost, boolean offense) {
		return true;
	}

	@Override
	public void ReportTheft(Artifact a, boolean gained) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean TilesAreTheSameUncertain(VisionData v1, VisionData v2)
	{
		if((v1.Cost == v2.Cost) &&
			(v1.Environment == v2.Environment) &&
			(v1.IsWater() == v2.IsWater()) &&
			(v1.IsForest() == v2.IsForest()) &&
			(v1.IsMountain() == v2.IsMountain()) &&
			(v1.IsHole() == v2.IsHole()) &&
			(v1.IsWall() == v2.IsWall()) &&
			(v1.IsIdol() == v2.IsIdol()) &&
			(v1.IsAltar() == v2.IsAltar()))
		{
			return true;
		}
		return false;
		
	}
	
	
	public boolean coordinateContains4directNeighbors(Pair<Integer,Integer> coord)
	{
		if(!this.visionContainsCoordinate(new Pair<Integer,Integer>(coord.Item1+1,coord.Item2)))
		{
			return false;
		}
		if(!this.visionContainsCoordinate(new Pair<Integer,Integer>(coord.Item1-1,coord.Item2)))
		{
			return false;
		}
		if(!this.visionContainsCoordinate(new Pair<Integer,Integer>(coord.Item1,coord.Item2+1)))
		{
			return false;
		}
		if(!this.visionContainsCoordinate(new Pair<Integer,Integer>(coord.Item1,coord.Item2-1)))
		{
			return false;
		}
		return true;
	}
	
	
	
	
	
	public Pair<Integer,Integer> whatIsOurCoord()
	{
		LinkedList<MyVertex> verts = new LinkedList<MyVertex>();
		int startid = 0;
		Type2Vertex center = this.getLocalVertexFromCoord(new Pair<Integer,Integer>(0,0));
		if(center.importantTileInfo.IsMountain())
		{
			return null;
		}
		Type2Vertex right = this.getLocalVertexFromCoord(new Pair<Integer,Integer>(1,0));
		Type2Vertex left = this.getLocalVertexFromCoord(new Pair<Integer,Integer>(-1,0));
		Type2Vertex up = this.getLocalVertexFromCoord(new Pair<Integer,Integer>(0,1));
		Type2Vertex down = this.getLocalVertexFromCoord(new Pair<Integer,Integer>(0,-1));
		GraphAlgorithms.BFS(MyBoardInfo,startid,(Graph,GraphId,Tree,TreeId) -> {
			MyVertex currentVertex = Graph.GetVertex(GraphId);
			Pair<Integer,Integer> currentcord = currentVertex.coordinate;
			if(this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1 + 1,currentcord.Item2)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1 - 1,currentcord.Item2)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 + 1)) &&
					this.visionContainsCoordinate(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 - 1)))
			{
				if(this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2)).importantTileInfo.Cost == center.importantTileInfo.Cost &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1 + 1,currentcord.Item2)).importantTileInfo.Cost == right.importantTileInfo.Cost  &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1 - 1,currentcord.Item2)).importantTileInfo.Cost == left.importantTileInfo.Cost  &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 + 1)).importantTileInfo.Cost == up.importantTileInfo.Cost &&
						this.getVertexfromCoord(new Pair<Integer,Integer>(currentcord.Item1,currentcord.Item2 - 1)).importantTileInfo.Cost == down.importantTileInfo.Cost)
						
				{
					verts.add(currentVertex);
				}
			}
		});
		if(verts.size() == 0)
		{
			return null;
		}
		return verts.Front().coordinate;
	}
	
	
	
	public void fixcoordinatesystem()
	{
		Pair<Integer,Integer> potentialcoord = this.whatIsOurCoordinate(currentInfoIG);
		if(potentialcoord != null)
		{
			currentLocation = potentialcoord;
		}
	}
	
	public Pair<Integer,Integer> whatIsOurCoordinate(IGraph<VisionData, Direction> robotvision)
	{
		
		LinkedList <Pair<Integer,Integer>> coords = getPotentialTilecoordinates(robotvision.GetVertex(0));
		if(coords.size() == 0)
		{
			//System.out.println("UnExplored, still lost (No matches)");
			return null;
		}
		else
		{
			//System.out.println("Might have been explored (still false positives in list)");
			//System.out.println("Potential COORDS " + coords.size());
			int neighborcounter = 0;
			for(@SuppressWarnings("unused") Integer data : robotvision.Neighbors(0))
			{
				neighborcounter++;
			}
			if(neighborcounter != 4)
			{
				//System.out.println("OUR COORD HAS NO VISION AND THIS ISNT GOOD");
				return null;
			}
			else
			{
				for(Pair<Integer,Integer> coord : coords)
				{
					if(!coordinateContains4directNeighbors(coord))
					{
						continue;//on to the next coord because this one was not a match
					}
					else
					{
						boolean isMatch = true;
						int hadValidNeighborCounter = 0;
						for(Integer id : robotvision.Neighbors(0))
						{
							Pair<Integer,Integer> r = new Pair<Integer,Integer>(coord.Item1+1,coord.Item2);
							Pair<Integer,Integer> l = new Pair<Integer,Integer>(coord.Item1-1,coord.Item2);
							Pair<Integer,Integer> u = new Pair<Integer,Integer>(coord.Item1,coord.Item2+1);
							Pair<Integer,Integer> d = new Pair<Integer,Integer>(coord.Item1,coord.Item2-1);
							if(!(this.TilesAreTheSameUncertain(this.getVertexfromCoord(r).importantTileInfo, robotvision.GetVertex(id)) ||
							this.TilesAreTheSameUncertain(this.getVertexfromCoord(l).importantTileInfo, robotvision.GetVertex(id)) ||
							this.TilesAreTheSameUncertain(this.getVertexfromCoord(u).importantTileInfo, robotvision.GetVertex(id)) ||
							this.TilesAreTheSameUncertain(this.getVertexfromCoord(d).importantTileInfo, robotvision.GetVertex(id))))
							{
								isMatch = false;
							}
							if(!isMatch)
							{
								break;
							}
							hadValidNeighborCounter++;
						}
						if(hadValidNeighborCounter == 4)
						{
							//System.out.println("OUR POTENTIAL COORD HAD 4 VALID NEIGHBORS!!!!!!");
							return coord;
						}
					}
				}
				//System.out.println("NONE OF OUR POTENTIAL COORDS MET THE CRITERIA");
				return null;
			}
		}
	}
	
	
	
	//check if we have been on this tile before (call this after we got teleported)
	public LinkedList <Pair<Integer,Integer>> getPotentialTilecoordinates(VisionData givenCenterData)
	{
		LinkedList <Pair<Integer,Integer>> potentialCoordinateList = new LinkedList <Pair<Integer,Integer>>();
		for(MyVertex vert : MyBoardInfo.Vertices())
		{
			if(TilesAreTheSameUncertain(givenCenterData,vert.importantTileInfo))
			{
				potentialCoordinateList.add(vert.coordinate); 
			}
		}
		return potentialCoordinateList;
	}
	
	
	@Override
	public void Teleported() {
		currentpath.Clear();
		currentLocation = null;
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Translate(Vector2d v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String GetName() {
		// TODO Auto-generated method stub
		return null;
	}

}
