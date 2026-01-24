package com.example.zenpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoardView extends View {
    private static final int MAX_GRID_SIZE = 8;
    private static final int MIN_GRID_SIZE = 4;
    private static final int BASE_CELL_SIZE = 80;

    private int gridCols;
    private int gridRows;
    private int cellSize;

    private Dot[][] grid;
    private List<GridPath> paths;
    private Paint boardPaint;
    private Paint gridPaint;
    private Paint pathPaint;
    private RectF boardRect;
    private float boardCornerRadius = 30f;

    private GridPath currentPath;
    private boolean isDrawing = false;
    private int currentLevel = 1;
    private boolean levelComplete = false;

    private int[] pastelColors = {
            R.color.pastel_lavender,
            R.color.pastel_sky_blue,
            R.color.pastel_mint,
            R.color.pastel_pink,
            R.color.pastel_peach,
            R.color.pastel_yellow
    };

    public GameBoardView(Context context) {
        super(context);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        updateGridSize(currentLevel);
        grid = new Dot[gridRows][gridCols];
        paths = new ArrayList<>();

        boardPaint = new Paint();
        boardPaint.setAntiAlias(true);
        boardPaint.setColor(ContextCompat.getColor(getContext(), R.color.game_board_bg));
        boardPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setAntiAlias(true);
        gridPaint.setColor(0x20000000); // Very subtle grid lines
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);

        pathPaint = new Paint();
        pathPaint.setAntiAlias(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(16f);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        boardRect = new RectF();
    }

    private void updateGridSize(int level) {
        if (level <= 0) level = 1;

        // Calculate grid size based on level
        int gridSize = MIN_GRID_SIZE + (level - 1);
        if (gridSize > MAX_GRID_SIZE) {
            gridSize = MAX_GRID_SIZE;
        }

        gridCols = gridSize;
        gridRows = gridSize;

        // Adjust cell size based on grid size to fit screen
        cellSize = BASE_CELL_SIZE;
        if (gridSize > 6) {
            cellSize = (int) (BASE_CELL_SIZE * 0.85); // Smaller cells for larger grids
        } else if (gridSize < 6) {
            cellSize = (int) (BASE_CELL_SIZE * 1.15); // Larger cells for smaller grids
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float boardWidth = gridCols * cellSize;
        float boardHeight = gridRows * cellSize;
        float left = (w - boardWidth) / 2f;
        float top = (h - boardHeight) / 2f;

        boardRect.set(left, top, left + boardWidth, top + boardHeight);
        generateLevel(currentLevel);
    }

    private void generateLevel(int level) {
        // Update grid size for new level
        updateGridSize(level);

        // Reinitialize grid with new dimensions
        grid = new Dot[gridRows][gridCols];

        // Clear existing data
        paths.clear();
        levelComplete = false;

        // Determine dot count based on level
        int totalPairs;
        int colorTypes;

        if (level == 1) {
            totalPairs = 3; // 6 dots total for Level 1
            colorTypes = 2; // 2 colors for simplicity
        } else {
            totalPairs = 4; // 8 dots total for Level 2+
            colorTypes = Math.min(2 + level / 3, 4); // Up to 4 colors
        }

        // Simple random placement
        generateSimpleLevel(totalPairs, colorTypes);

        invalidate();
    }

    private void generateSimpleLevel(int totalPairs, int colorTypes) {
        List<Integer> colorSequence = new ArrayList<>();
        for (int i = 0; i < totalPairs; i++) {
            int colorType = i % colorTypes;
            colorSequence.add(colorType);
            colorSequence.add(colorType);
        }

        java.util.Collections.shuffle(colorSequence);

        List<int[]> emptyCells = new ArrayList<>();
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                emptyCells.add(new int[]{row, col});
            }
        }
        java.util.Collections.shuffle(emptyCells);

        for (int i = 0; i < colorSequence.size() && i < emptyCells.size(); i++) {
            int[] cell = emptyCells.get(i);
            int row = cell[0];
            int col = cell[1];
            int colorType = colorSequence.get(i);
            int color = ContextCompat.getColor(getContext(), pastelColors[colorType]);

            placeDotOnGrid(new GridPosition(row, col), color, colorType);
        }
    }

    private void placeDotOnGrid(GridPosition pos, int color, int colorType) {
        float x = boardRect.left + pos.col * cellSize + cellSize / 2f;
        float y = boardRect.top + pos.row * cellSize + cellSize / 2f;
        int dotRadius = (int) (cellSize * 0.25);

        grid[pos.row][pos.col] = new Dot(x, y, dotRadius, color, colorType);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw rounded board background
        canvas.drawRoundRect(boardRect, boardCornerRadius, boardCornerRadius, boardPaint);

        // Draw grid lines
        for (int row = 0; row <= gridRows; row++) {
            float y = boardRect.top + row * cellSize;
            canvas.drawLine(boardRect.left, y, boardRect.right, y, gridPaint);
        }
        for (int col = 0; col <= gridCols; col++) {
            float x = boardRect.left + col * cellSize;
            canvas.drawLine(x, boardRect.top, x, boardRect.bottom, gridPaint);
        }

        // Draw completed paths
        for (GridPath path : paths) {
            pathPaint.setColor(path.getColor());
            canvas.drawPath(path.getPath(), pathPaint);
        }

        // Draw current path
        if (isDrawing && currentPath != null) {
            pathPaint.setColor(currentPath.getColor());
            canvas.drawPath(currentPath.getPath(), pathPaint);
        }

        // Draw dots
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (grid[row][col] != null) {
                    grid[row][col].draw(canvas);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x, y);
                return true;

            case MotionEvent.ACTION_UP:
                handleTouchUp();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(float x, float y) {
        GridPosition pos = getGridPosition(x, y);
        if (pos != null && grid[pos.row][pos.col] != null && !grid[pos.row][pos.col].isConnected()) {
            Dot startDot = grid[pos.row][pos.col];
            currentPath = new GridPath(startDot.getColor());
            currentPath.addPosition(pos);
            isDrawing = true;
        }
    }

    private void handleTouchMove(float x, float y) {
        if (!isDrawing || currentPath == null) return;

        GridPosition pos = getGridPosition(x, y);
        if (pos != null) {
            GridPosition lastPos = currentPath.getLastPosition();

            // Check if adjacent (up, down, left, right only)
            if (isAdjacent(lastPos, pos) && !currentPath.containsPosition(pos)) {
                // Check if path would cross existing paths
                if (!wouldPathCross(currentPath, pos)) {
                    currentPath.addPosition(pos);
                    updatePath(currentPath);
                    invalidate();
                }
            }
        }
    }

    private void handleTouchUp() {
        if (!isDrawing || currentPath == null) return;

        GridPosition startPos = currentPath.getPosition(0);
        GridPosition endPos = currentPath.getLastPosition();

        // Check if path connects matching dots
        if (grid[startPos.row][startPos.col] != null && grid[endPos.row][endPos.col] != null) {
            Dot startDot = grid[startPos.row][startPos.col];
            Dot endDot = grid[endPos.row][endPos.col];

            if (startDot.getColorType() == endDot.getColorType()) {
                // Valid connection - lock it in
                paths.add(currentPath);
                startDot.setConnected(true);
                endDot.setConnected(true);

                checkLevelComplete();
            }
        }

        isDrawing = false;
        currentPath = null;
        invalidate();
    }

    private GridPosition getGridPosition(float x, float y) {
        if (!boardRect.contains(x, y)) return null;

        int col = (int) ((x - boardRect.left) / cellSize);
        int row = (int) ((y - boardRect.top) / cellSize);

        if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
            return new GridPosition(row, col);
        }
        return null;
    }

    private boolean isAdjacent(GridPosition pos1, GridPosition pos2) {
        int rowDiff = Math.abs(pos1.row - pos2.row);
        int colDiff = Math.abs(pos1.col - pos2.col);
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
    }

    private boolean wouldPathCross(GridPath path, GridPosition newPos) {
        // Simple implementation - check if new position conflicts with existing paths
        for (GridPath existingPath : paths) {
            if (existingPath.containsPosition(newPos)) {
                return true;
            }
        }
        return false;
    }

    private void updatePath(GridPath path) {
        Path androidPath = new Path();
        List<GridPosition> positions = path.getPositions();

        if (positions.size() > 0) {
            GridPosition firstPos = positions.get(0);
            androidPath.moveTo(boardRect.left + firstPos.col * cellSize + cellSize / 2f,
                    boardRect.top + firstPos.row * cellSize + cellSize / 2f);

            for (int i = 1; i < positions.size(); i++) {
                GridPosition pos = positions.get(i);
                androidPath.lineTo(boardRect.left + pos.col * cellSize + cellSize / 2f,
                        boardRect.top + pos.row * cellSize + cellSize / 2f);
            }
        }

        path.setPath(androidPath);
    }

    private void checkLevelComplete() {
        // Count total dots and connected dots
        int totalDots = 0;
        int connectedDots = 0;

        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (grid[row][col] != null) {
                    totalDots++;
                    if (grid[row][col].isConnected()) {
                        connectedDots++;
                    }
                }
            }
        }

        // Level is complete when all dots are connected
        if (totalDots > 0 && connectedDots == totalDots) {
            levelComplete = true;
        }
    }

    public void setCurrentLevel(int level) {
        this.currentLevel = level;
        generateLevel(level);
    }

    public void resetGame() {
        generateLevel(currentLevel);
    }

    public boolean isLevelComplete() {
        return levelComplete;
    }

    public int getCurrentGridSize() {
        return gridCols; // gridCols equals gridRows
    }

    private static class GridPosition {
        int row, col;

        GridPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GridPosition that = (GridPosition) obj;
            return row == that.row && col == that.col;
        }
    }

    private static class GridPath {
        private List<GridPosition> positions;
        private Path path;
        private int color;

        GridPath(int color) {
            this.positions = new ArrayList<>();
            this.path = new Path();
            this.color = color;
        }

        void addPosition(GridPosition pos) {
            positions.add(pos);
        }

        GridPosition getLastPosition() {
            return positions.get(positions.size() - 1);
        }

        GridPosition getPosition(int index) {
            return positions.get(index);
        }

        boolean containsPosition(GridPosition pos) {
            return positions.contains(pos);
        }

        List<GridPosition> getPositions() {
            return positions;
        }

        Path getPath() {
            return path;
        }

        void setPath(Path path) {
            this.path = path;
        }

        int getColor() {
            return color;
        }
    }
}
