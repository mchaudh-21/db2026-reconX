// TICKET-ADV114 — Compound <DataTable> with Header / Body / Pagination subcomponents.
import {
  createContext,
  useContext,
  useMemo,
  useState,
} from 'react';

const DataTableContext = createContext(null);

function useDataTable() {
  const context = useContext(DataTableContext);

  if (!context) {
    throw new Error(
      'DataTable sub-components must be used inside <DataTable>',
    );
  }

  return context;
}

export function DataTable({
  data = [],
  children,
  pageSize = 10,
}) {
  const [sort, setSort] = useState({
    key: null,
    direction: 'asc',
  });
  const [page, setPage] = useState(0);

  const sortedData = useMemo(() => {
    if (!sort.key) {
      return data;
    }

    return [...data].sort((left, right) => {
      const leftValue = left[sort.key];
      const rightValue = right[sort.key];

      if (leftValue === rightValue) {
        return 0;
      }

      const comparison = leftValue > rightValue ? 1 : -1;

      return sort.direction === 'asc'
        ? comparison
        : -comparison;
    });
  }, [data, sort]);

  const totalPages = Math.max(
    1,
    Math.ceil(sortedData.length / pageSize),
  );

  const visibleRows = sortedData.slice(
    page * pageSize,
    page * pageSize + pageSize,
  );

  function handleSort(key) {
    setPage(0);

    setSort((previousSort) => {
      if (previousSort.key === key) {
        return {
          key,
          direction:
            previousSort.direction === 'asc'
              ? 'desc'
              : 'asc',
        };
      }

      return {
        key,
        direction: 'asc',
      };
    });
  }

  const value = {
    page,
    setPage,
    sort,
    handleSort,
    totalPages,
    visibleRows,
  };

  return (
    <DataTableContext.Provider value={value}>
      <div className="data-table">{children}</div>
    </DataTableContext.Provider>
  );
}

function DataTableHeader({ columns }) {
  const { sort, handleSort } = useDataTable();

  return (
    <div className="data-table__header" role="row">
      {columns.map((column) => {
        const active = sort.key === column.key;

        return (
          <button
            key={column.key}
            type="button"
            className="data-table__header-cell"
            onClick={() => handleSort(column.key)}
            aria-sort={
              active
                ? sort.direction === 'asc'
                  ? 'ascending'
                  : 'descending'
                : 'none'
            }
          >
            {column.label}
          </button>
        );
      })}
    </div>
  );
}

function DataTableBody({ renderRow }) {
  const { visibleRows } = useDataTable();

  return (
    <div className="data-table__body">
      {visibleRows.map((row, index) =>
        renderRow(row, index),
      )}
    </div>
  );
}

function DataTablePagination() {
  const {
    page,
    setPage,
    totalPages,
  } = useDataTable();

  return (
    <nav
      className="data-table__pagination"
      aria-label="Table pagination"
    >
      <button
        type="button"
        onClick={() => setPage((current) => current - 1)}
        disabled={page === 0}
      >
        Previous
      </button>

      <span>
        Page {page + 1} of {totalPages}
      </span>

      <button
        type="button"
        onClick={() => setPage((current) => current + 1)}
        disabled={page >= totalPages - 1}
      >
        Next
      </button>
    </nav>
  );
}

DataTable.Header = DataTableHeader;
DataTable.Body = DataTableBody;
DataTable.Pagination = DataTablePagination;

export default DataTable;
