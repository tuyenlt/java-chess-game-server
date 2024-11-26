package chessgameserver.logic;
import java.util.List;
import java.util.ArrayList;

public abstract class Piece {

    protected String pieceColor;
    protected String name = "p"; 

    public Piece(String pieceColor) {
        this.pieceColor = pieceColor;
    }
    public String getpieceColor(){
        return pieceColor;
    }
    
    public abstract List<Move> getValidMoves(Board board, int startRow, int startCol);

    public List<Move> getSafeMoves(Board board, int startRow, int startCol) {
        List<Move> safeMoves = new ArrayList<>(); 
        for(Move move : getValidMoves(board, startRow, startCol)){
            if(Utils.isSafeMove(board, pieceColor, move)){
                safeMoves.add(move);
            }
        }
        return safeMoves;
    }

    public boolean canAtack(Board board, int row, int col, int targetRow, int targetCol){
        for(Move move : getValidMoves(board, row, col)){
            if(move.getEndRow()==targetRow && move.getEndCol()==targetCol){
                return true;
            }
        }
        return false;
    }
    
    public String getName(){
        return pieceColor + name.toUpperCase();
    }
}


class Pawn extends Piece {
    public Pawn(String pieceColor) {
        super(pieceColor);
        name = "p";
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {
        List<Move> validMoves = new ArrayList<>();
        int direction = (pieceColor.equals("w")) ? -1 : 1;

        if (Utils.isEmpty(board, startRow + direction, startCol)) {
            validMoves.add(new Move(startRow, startCol, startRow + direction, startCol));
            int initialRow = (pieceColor.equals("w")) ? 6 : 1;
            if (startRow == initialRow && Utils.isEmpty(board, startRow + 2 * direction, startCol)) {
                validMoves.add(new Move(startRow, startCol, startRow + 2 * direction, startCol));
            }
        }

        for (int offset : new int[]{-1, 1}) {
            int endCol = startCol + offset;
            if (Utils.isMoveAllowed(board, pieceColor, startRow + direction, endCol) 
                && !Utils.isEmpty(board, startRow + direction, endCol)) {
                validMoves.add(new Move(startRow, startCol, startRow + direction, endCol));
            }
        }

        // logic tốt qua đường
        Move enPassantMove = Utils.canEnPassant(board, pieceColor, startRow, startCol);
        if(enPassantMove != null){
            validMoves.add(enPassantMove);
        }

        return validMoves; 
    }
}

class Rook extends Piece {
    private boolean hasMoved = false;
    
    public Rook(String pieceColor) {
        super(pieceColor);
        name = "r";
    }
    
    public boolean isMove() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {
        List<Move> validMoves = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        for (int [] direction : directions){
            int endRow = startRow + direction[0];
            int endCol = startCol + direction[1];
            while (Utils.isMoveAllowed(board, pieceColor, endRow, endCol)) {
                validMoves.add(new Move(startRow, startCol, endRow, endCol));
                if (!Utils.isEmpty(board, endRow, endCol)){
                    break;
                }
                endRow += direction[0];
                endCol += direction[1];
            }
        }
        return validMoves;
    }
}


class Knight extends Piece {
    public Knight(String pieceColor) {
        super(pieceColor);
        name = "n";
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {
        List<Move> validMoves = new ArrayList<>();
        int[][] directions = {{-2, -1}, {-2, 1}, {-1, 2}, {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}};
        
        for (int [] direction : directions){
            int endRow = startRow + direction[0];
            int endCol = startCol + direction[1];
            if (Utils.isMoveAllowed(board, pieceColor, endRow, endCol)){
                validMoves.add(new Move(startRow, startCol, endRow, endCol));
            }
        }
        return validMoves;
    }
}

class Bishop extends Piece {
    public Bishop(String pieceColor) {
        super(pieceColor);
        name = "b";
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {
        List<Move> validMoves = new ArrayList<>();
        int[][] directions = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};

        for (int [] direction : directions){
            int endRow = startRow + direction[0];
            int endCol = startCol + direction[1];
            while (Utils.isMoveAllowed(board, pieceColor, endRow, endCol)) {
                validMoves.add(new Move(startRow, startCol, endRow, endCol));
                if (!Utils.isEmpty(board, endRow, endCol)){
                    break;
                }
                endRow += direction[0];
                endCol += direction[1];
            }
        }
        return validMoves;
    }
}

class Queen extends Piece {
    public Queen(String pieceColor) {
        super(pieceColor);
        name = "q";
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {
        List<Move> validMoves = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};

        for (int [] direction : directions){
            int endRow = startRow + direction[0];
            int endCol = startCol + direction[1];
            while (Utils.isMoveAllowed(board, pieceColor, endRow, endCol)) {
                validMoves.add(new Move(startRow, startCol, endRow, endCol));
                if (!Utils.isEmpty(board, endRow, endCol)){
                    break;
                }
                endRow += direction[0];
                endCol += direction[1];
            }
        }
        return validMoves;
    }
}

class King extends Piece {
    private boolean hasMoved = false;

    public King(String pieceColor) {
        super(pieceColor);
        name = "k";
    }

    public boolean isMove() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean getHasMoved(){
        return hasMoved;
    }

    @Override
    public List<Move> getValidMoves(Board board, int startRow, int startCol) {        
        List<Move> validMoves = new ArrayList<>();
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, 1},{1, 1}, {1, 0}, {1, -1}, {0, -1}};
        for (int [] direction : directions){

            int endRow = startRow + direction[0];
            int endCol = startCol + direction[1];
            if (Utils.isMoveAllowed(board, pieceColor, endRow, endCol)){
                validMoves.add(new Move(startRow, startCol, endRow, endCol));
            }
        }
        return validMoves;
    }
    public List<Move> getSafeMoves(Board board, int startRow,int startCol){
        List<Move> safeMoves = super.getSafeMoves(board, startRow, startCol);
        if (Utils.canCastleLeft(board, startRow, startCol, hasMoved, pieceColor)){
            safeMoves.add(new Move(startRow, startCol, startRow, 2));
        }
        if (Utils.canCastleRight(board, startRow, startCol, hasMoved, pieceColor)) {
            safeMoves.add(new Move(startRow, startCol, startRow, 6));
        }
        return safeMoves;
    }
    
}
