// TICKET-ADV114 — Compound <DataTable> with Header / Body / Pagination subcomponents.
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

const DEFAULT_PAGE_SIZE = 10;
const EMPTY_ROWS = [];
const DATE_PREFIX = /^\d{4}-\d{2}-\d{2}/;
const stringCollator = new Intl.Collator(undefined, {
  numeric: true,
  sensitivity: 'base',
});
const DataTableContext = createContext(null);

function normalizePageSize(pageSize) {
  const normalized = Number(pageSize);
  return Number.isFinite(normalized) && normalized > 0
    ? Math.floor(normalized)
    : DEFAULT_PAGE_SIZE;
}

function comparePrimitive(left, right) {
  if (left === right) return 0;
  return left < right ? -1 : 1;
}

function isDateLike(value) {
  return value instanceof Date || (typeof value === 'string' && DATE_PREFIX.test(value));
}

function toTimestamp(value) {
  const timestamp = value instanceof Date ? value.getTime() : Date.parse(String(value));
  return Number.isFinite(timestamp) ? timestamp : null;
}

function compareValues(left, right, type) {
  const normalizedType = typeof type === 'string' ? type.toLowerCase() : '';

  if (normalizedType === 'number' || (typeof left === 'number' && typeof right === 'number')) {
    const leftNumber = Number(left);
    const rightNumber = Number(right);
    if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
      return comparePrimitive(leftNumber, rightNumber);
    }
  }

  if (
    normalizedType === 'date'
    || left instanceof Date
    || right instanceof Date
    || (isDateLike(left) && isDateLike(right))
  ) {
    const leftTimestamp = toTimestamp(left);
    const rightTimestamp = toTimestamp(right);
    if (leftTimestamp !== null && rightTimestamp !== null) {
      return comparePrimitive(leftTimestamp, rightTimestamp);
    }
  }

  return stringCollator.compare(String(left), String(right));
}

function compareRows(left, right, sort) {
  const leftValue = left.row == null ? undefined : left.row[sort.key];
  const rightValue = right.row == null ? undefined : right.row[sort.key];
  const leftMissing = leftValue === null || leftValue === undefined;
  const rightMissing = rightValue === null || rightValue === undefined;

  // Missing backend values are consistently placed last in either direction.
  if (leftMissing || rightMissing) {
    if (leftMissing && rightMissing) return left.sourceIndex - right.sourceIndex;
    return leftMissing ? 1 : -1;
  }

  const result = compareValues(leftValue, rightValue, sort.type);
  if (result !== 0) return sort.direction === 'asc' ? result : -result;
  return left.sourceIndex - right.sourceIndex;
}

function useDataTable() {
  const context = useContext(DataTableContext);
  if (context === null) {
    throw new Error('DataTable subcomponents must be rendered inside <DataTable>.');
  }
  return context;
}

function defaultRowId(row) {
  if (row !== null && typeof row === 'object') {
    if (row.id !== null && row.id !== undefined) return row.id;
    if (row.tradeRef !== null && row.tradeRef !== undefined) return row.tradeRef;
  }
  return null;
}

export default function DataTable({
  children,
  data = EMPTY_ROWS,
  pageSize = DEFAULT_PAGE_SIZE,
  getRowId,
}) {
  const rows = Array.isArray(data) ? data : EMPTY_ROWS;
  const normalizedPageSize = normalizePageSize(pageSize);
  const [sort, setSort] = useState(null);
  const [page, setPage] = useState(0);
  const generatedIds = useRef(new WeakMap());
  const nextGeneratedId = useRef(0);

  const rowsWithIds = useMemo(() => rows.map((row, sourceIndex) => {
    let rowId = getRowId ? getRowId(row, sourceIndex) : defaultRowId(row);

    if (rowId === null || rowId === undefined) {
      const canUseObjectIdentity = (typeof row === 'object' && row !== null) || typeof row === 'function';
      if (canUseObjectIdentity) {
        if (!generatedIds.current.has(row)) {
          nextGeneratedId.current += 1;
          generatedIds.current.set(row, `generated-${nextGeneratedId.current}`);
        }
        rowId = generatedIds.current.get(row);
      } else {
        rowId = `row-${sourceIndex}`;
      }
    }

    return { row, rowId: String(rowId), sourceIndex };
  }), [getRowId, rows]);

  const sortedRows = useMemo(() => {
    if (sort === null) return rowsWithIds;
    return [...rowsWithIds].sort((left, right) => compareRows(left, right, sort));
  }, [rowsWithIds, sort]);

  const totalPages = Math.max(1, Math.ceil(rowsWithIds.length / normalizedPageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pageRows = useMemo(
    () => sortedRows.slice(
      currentPage * normalizedPageSize,
      (currentPage + 1) * normalizedPageSize,
    ),
    [currentPage, normalizedPageSize, sortedRows]
  );
  const visibleRows = useMemo(() => pageRows.map(({ row }) => row), [pageRows]);

  useEffect(() => {
    setPage((currentPageState) => Math.min(currentPageState, totalPages - 1));
  }, [rowsWithIds, normalizedPageSize, totalPages]);

  const sortBy = useCallback((column) => {
    setSort((currentSort) => ({
      key: column.key,
      type: column.type ?? column.sortType,
      direction: currentSort?.key === column.key && currentSort.direction === 'asc' ? 'desc' : 'asc',
    }));
    setPage(0);
  }, []);

  const goToPage = useCallback((requestedPage) => {
    setPage(Math.min(Math.max(requestedPage, 0), totalPages - 1));
  }, [totalPages]);

  const contextValue = useMemo(() => ({
    rows: visibleRows,
    rowEntries: pageRows,
    totalRows: rowsWithIds.length,
    page: currentPage,
    pageSize: normalizedPageSize,
    totalPages,
    sort,
    sortBy,
    goToPage,
  }), [
    currentPage,
    goToPage,
    normalizedPageSize,
    pageRows,
    rowsWithIds.length,
    sort,
    sortBy,
    totalPages,
    visibleRows,
  ]);

  return (
    <DataTableContext.Provider value={contextValue}>
      <div className="data-table" role="table">
        {children}
      </div>
    </DataTableContext.Provider>
  );
}

DataTable.Header = function Header({ columns = [] }) {
  const { sort, sortBy } = useDataTable();
  const safeColumns = Array.isArray(columns) ? columns : [];

  return (
    <div className="data-table__header" role="row">
      {safeColumns.map((column) => {
        const isActive = sort?.key === column.key;
        const direction = isActive ? sort.direction : null;
        const ariaSort = direction === 'asc'
          ? 'ascending'
          : direction === 'desc'
            ? 'descending'
            : 'none';

        return (
          <div
            key={column.key}
            role="columnheader"
            aria-sort={ariaSort}
          >
            <button
              type="button"
              className={`data-table__th data-table__th--${isActive ? 'active' : 'idle'}`}
              aria-pressed={isActive}
              onClick={() => sortBy(column)}
            >
              {column.label ?? column.key}
            </button>
          </div>
        );
      })}
    </div>
  );
};

DataTable.Body = function Body({ renderRow, render }) {
  const { rowEntries } = useDataTable();
  const renderItem = renderRow ?? render;

  if (typeof renderItem !== 'function') {
    throw new Error('DataTable.Body requires a renderRow function.');
  }

  return (
    <div className="data-table__body" role="rowgroup">
      {rowEntries.map(({ row, rowId }) => {
        const renderedRow = renderItem(row);
        const isRowElement = React.isValidElement(renderedRow)
          && (
            renderedRow.type === 'tr'
            || renderedRow.props.role === 'row'
            || renderedRow.type?.dataTableRow === true
          );

        if (isRowElement) {
          return React.cloneElement(renderedRow, { key: rowId });
        }

        return (
          <div key={rowId} className="data-table__row" role="row" data-row-id={rowId}>
            {renderedRow}
          </div>
        );
      })}
    </div>
  );
};

DataTable.Pagination = function Pagination() {
  const { page, totalPages, goToPage } = useDataTable();

  return (
    <nav className="data-table__pagination" aria-label="Pagination">
      <button
        type="button"
        aria-label="Previous page"
        disabled={page === 0}
        onClick={() => goToPage(page - 1)}
      >
        ‹
      </button>
      <span aria-live="polite">Page {page + 1} of {totalPages}</span>
      <button
        type="button"
        aria-label="Next page"
        disabled={page >= totalPages - 1}
        onClick={() => goToPage(page + 1)}
      >
        ›
      </button>
    </nav>
  );
};
