package board;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class Main
{
	private final Map<Position, Tromino> trominoes = new HashMap<>();
	private final Position deficient;
	private final Position maximumAllowedPosition;

	/**
	 * Create a new board to be tiled.
	 * 
	 * @param n The board size in squares. Must be a power of 2.
	 * @param missing The missing square on the board.
	 * 
	 * These restrictions are necessary to guarantee a tiling of the board.
	 */
	public Main(int n, Position missing)
	{
		deficient = missing;
		maximumAllowedPosition = new Position (n, n);
	}

	/**
	 * Add a given tromino to a given position.
	 *
	 * Using this function allows the caller to (for example):
	 * Tile their own board
	 * Partially tile a board then call tile() to have the 
	 * algorithm finish the board, if possible.
	 * 
	 * 
	 * No error checking is done on the provided position or tromino.
	 * No guarantees are given to the tileability of the modified board.
	 * 
	 * @param t The trominio to add.
	 * @param p The position to add the tromino to.
	 * 
	 * See Tromino.java for an explanation of tromino characteristics. 
	 */
	public void addTromino(Tromino t, Position p)
	{
		trominoes.put(p, t);
	}

	/**
	 * Automatically tile the current board.
	 */
	public void tile()
	{
		tile(new Position(0,0), maximumAllowedPosition);
	}

	/**
	 * A well known divide and conquer algorithm for tiling deficient boards.
	 *
	 * @param start The starting position of the region as defined by Position.between.
	 * @param end The ending position of the region as defined by Position.between.
	 */
	private void tile(Position start, Position end)
	{
		if (isTwoByTwo(start, end))
		{
			Optional<Pair<Position,Tromino>> t = tileTwoByTwo(start, end);
			trominoes.put(t.get().left(), t.get().right());
		}
		else
		{	
			Position middle = new Position((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);

			Position TLS = new Position(start.getX(), middle.getY());
			Position TLE = new Position(middle.getX(), end.getY());

			Position TRS = middle;
			Position TRE = end;

			Position LLS = start;
			Position LLE = middle;

			Position LRS = new Position(middle.getX(), start.getY());
			Position LRE = new Position(end.getX(), middle.getY());


			if (hasADeficientSquare(TLS, TLE))
			{
				addTromino(Tromino.LR, middle);
			}
			else if (hasADeficientSquare(TRS, TRE))
			{
				addTromino(Tromino.LL, middle);
			}
			else if (hasADeficientSquare(LLS, LLE))
			{
				addTromino(Tromino.UR, middle);
			}
			else if (hasADeficientSquare(LRS, LRE))
			{
				addTromino(Tromino.UL, middle);
			}
			else
			{
				throw new IllegalArgumentException("Area does not have a deficient square");
			}

			tile(TLS, TLE);
			tile(TRS, TRE);
			tile(LLS, LLE);
			tile(LRS, LRE);
		}
	}

	/**
	 * Determine whether there is a deficient square within a range.
	 * See also findDeficientSquare.
	 * @param start The starting position as defined by findDeficientSquare
	 * @param end The ending position as defined by findDeficientSquare
	 * @return True iff a deficient square is present within the range, false otherwise.
	 */
	private boolean hasADeficientSquare(Position start, Position end)
	{
		return findDeficientSquare(start, end).isPresent();
	}

	public Map<Position, Tromino> getTrominoes()
	{
		return trominoes;
	}

	/**
	 * Tile a deficient two by two square.
	 * 
	 * @throws IllegalArgumentException Iff the provided square is not two by two.
	 * @throws IllegalStateException Iff the square does not contain a deficient square.
	 * @param start The starting position of the tiles.
	 * @param end The ending position of the tiles.
	 * @return The position and tromino required to tile the provided two by two square.
	 */
	public Optional<Pair<Position, Tromino>> tileTwoByTwo(Position start, Position end)
	{
		if (!isTwoByTwo(start, end))
		{
			throw new IllegalArgumentException("Not a 2x2");
		}

		Position center = new Position(start.getX() + end.getX() - start.getX() - 1, start.getY() + end.getY() - start.getY() - 1);

		Optional<Position> deficientSquare = findDeficientSquare(start, end);

		if (!deficientSquare.isPresent())
		{
			throw new IllegalStateException("2x2 does not have a deficient square!");
		}

		Tromino toAdd = noOverlap(deficientSquare.get(), center);
		return Optional.of(new Pair<Position, Tromino> (center, toAdd));
	}

	/**
	 * Find a deficient square within a block.
	 * 
	 * There are two types of deficient square:
	 * The square that is introduced on creation of the board, this is unique
	 * and is stored in `deficient`.
	 * 
	 * A single square from another tromino that is within the space created
	 * by `start` and `end` - this is guaranteed to be present via the algorithm
	 * but the caller checks for presence.
	 * 
	 * @param start The starting position of the area that contains a deficient square.
	 * @param end The ending position of the area that contains a deficient square.
	 * @return The only deficient square within the area as defined by Position.between.
	 */
	private Optional<Position> findDeficientSquare(Position start, Position end)
	{
		// THE deficient square
		if (deficient.between(start, end))
		{
			return Optional.of(deficient);
		}

		// A part of a tromino is the deficient piece in our square
		for (Entry<Position, Tromino> e : trominoes.entrySet())
		{
			for (Position p : e.getValue().apply(e.getKey()))
			{
				if (p.between(start, end))
				{
					return Optional.of(p);
				}
			}
		}

		// There isn't one
		return Optional.empty();
	}

	/**
	 * Determine whether the shape produced by a start and end point is a two by two square.
	 * Squares made on the negative axis are treated as invalid and not a two by two square.
	 * 
	 * @param start The starting position of the square.
	 * @param end The ending position of the square.
	 * @return True iff the square positions are positive and the square is a two by two.
	 */
	private boolean isTwoByTwo(Position start, Position end)
	{
		return end.getX() - start.getX() == 2 && end.getY() - start.getY() == 2;
	}

	/**
	 * If the algorithm is implemented correctly there is a 
	 * tromino that does not overlap.
	 * @param d The location of the deficient square.
	 * @param p The center of the tromino to add.
	 * @return A tromino that does not overlap the deficient square starting at p.
	 */
	private Tromino noOverlap(Position d, Position p)
	{
		for (Tromino t : Tromino.values())
		{
			if (!overlaps(t, p, d))
			{
				return t;
			}
		}
		
		return null;
	}

	private boolean overlaps(Tromino t, Position p, Position d)
	{
		List<Position> trominoPieces = t.apply(p);

		for (Position tP : trominoPieces)
		{
			if (trominoes.get(tP) != null || d.equals(tP))
			{
				return true;
			}
		}

		return false;
	}

	public static void main(String[] args)
	{
		try
		{
			int boardSize = Integer.parseInt(args[0]);
			int missingSquareX = Integer.parseInt(args[1]);
			int missingSquareY = Integer.parseInt(args[2]);
			
			Position deficient = new Position(missingSquareX, missingSquareY);
			
			if (!positivePowerOfTwo(boardSize))
			{
				printAndExit("Board size must be a positive power of two.", -3);
			}
			
			if (!withinBounds(deficient, boardSize))
			{
				printAndExit("Missing square must be within the board.", -4);
			}
			
			new View(boardSize, deficient);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			printAndExit("Missing arguments.\n" + usageInformation(), -1);
		}
		catch (NumberFormatException e)
		{
			printAndExit("Couldn't parse input.\n" + usageInformation(), -2);
		}
	}
	
	private static boolean withinBounds(Position deficient, int boardSize)
	{
		return deficient.between(new Position(0,0), new Position(boardSize, boardSize));
	}

	private static boolean positivePowerOfTwo(int n)
	{
		// https://stackoverflow.com/questions/19383248/find-if-a-number-is-a-power-of-two-without-math-function-or-log-function
		return (n > 0) && ((n & (n - 1)) == 0);
	}

	private static void printAndExit(String message, int exitCode)
	{
		System.err.println(message);
		System.exit(exitCode);
	}
	
	private static String usageInformation()
	{
		return ""
				+ "Usage: boardSize missingSquareX missingSquareY\n"
				+ "boardSize is the width and height of the board. Must be a power of two >= 2.\n"
				+ "missingSquareX and missingSquareY are the x and y coords of the deficient square.\n"
				+ "For example, in a 2x2 square with the deficient square in the bottom left the call would be:\n"
				+ "2 0 0\n"
				+ "Similarly, a 2x2 with the square in the top right woule be:\n"
				+ "2 1 1";
	}
}
