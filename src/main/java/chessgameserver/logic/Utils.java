package chessgameserver.logic;

public class Utils {

    // Kiểm tra trạng thái của ô
    static boolean isEmpty(Board board, int row, int col) {
        return board.isEmpty(row, col);
    }

    // Kiểm tra điều kiện di chuyển của quân cờ
    static boolean isMoveAllowed(Board board, String pieceColor, int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return false;
        if (isEmpty(board, row, col)) return true;
        return !board.getPiece(row, col).getpieceColor().equals(pieceColor);
    }

    // Kiểm tra xem ô (row, col) có đang bị tấn công không
    static boolean isUnderAttack(Board board, String pieceColor, int targetRow, int targetCol) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getpieceColor().equals(pieceColor)) continue;
                if (piece.canAtack(board, row, col, targetRow, targetCol)) return true;
            }
        }
        return false;
    }

    // Kiểm tra trạng thái bị chiếu của King
    static boolean isCheck(Board board, String pieceColor) {
        int kingRow = board.getKingRow(pieceColor);
        int kingCol = board.getKingCol(pieceColor);
        return isUnderAttack(board, pieceColor, kingRow, kingCol);
    }

    // Kiểm tra nếu một nước đi là an toàn cho quân cờ
    static boolean isSafeMove(Board board, String pieceColor, Move move) {
        Piece originalPiece = board.getPiece(move.getEndRow(), move.getEndCol());

        // Di chuyển giả
        board.movePiece(move, true);
        boolean safe = !isCheck(board, pieceColor);

        // Khôi phục lại
        board.movePiece(move.getReverseMove(), true);
        board.setPiece(move.getStartRow(), move.getStartCol(), originalPiece);

        if (move.isPromotion(board)) {
            board.setPiece(move.getStartRow(), move.getStartCol(), new Pawn(pieceColor));
        }

        return safe;
    }

    // Kiểm tra điều kiện nhập thành trái
    static boolean canCastleLeft(Board board, int startRow, int startCol, boolean hasMoved, String pieceColor) {
        if (hasMoved || isCheck(board, pieceColor)) return false;
        int initialRow = (pieceColor.equals("w") ? 7 : 0);
        Piece piece = board.getPiece(initialRow, 0);    
        if (!(piece instanceof Rook) || !piece.getpieceColor().equals(pieceColor)) return false;
        Rook rook = (Rook) piece;
        if (rook.isMove()) return false;
        for (int col = 1; col < 4; col++) {
            if (!isEmpty(board, initialRow, col) || isUnderAttack(board, pieceColor, initialRow, col)) {
                return false;
            }
        }
        return true;
    }

    // Kiểm tra điều kiện nhập thành phải
    static boolean canCastleRight(Board board, int startRow, int startCol, boolean hasMoved, String pieceColor) {
        int initialRow = (pieceColor.equals("w") ? 7 : 0);
        if (hasMoved || isCheck(board, pieceColor)) return false;
        Piece piece = board.getPiece(initialRow, 7);
        if (!(piece instanceof Rook) || !piece.getpieceColor().equals(pieceColor)) return false;
        Rook rook = (Rook) piece;
        if (rook.isMove()) return false;
        for (int col = 5; col < 7; col++) {
            if (!isEmpty(board, initialRow, col) || isUnderAttack(board, pieceColor, initialRow, col)) {
                return false;
            }
        }
        return true;
    }
}
