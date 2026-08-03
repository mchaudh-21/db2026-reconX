import { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import TradeRow from '@components/TradeRow.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [selectedId, setSelectedId] = useState(null);
  const [data, setData] = useState({
    items: [],
    totalPages: 0,
  });

  useEffect(() => {
    let cancelled = false;

    async function loadTrades() {
      try {
        const response = await api.listTrades({
          page: 0,
          size: 20,
          status: debounced,
        });

        if (!cancelled) {
          setData({
            items: response.items ?? response.content ?? [],
            totalPages: response.totalPages ?? 0,
          });
        }
      } catch {
        if (!cancelled) {
          setData({
            items: [],
            totalPages: 0,
          });
        }
      }
    }

    loadTrades();

    return () => {
      cancelled = true;
    };
  }, [debounced]);

  const handleSelect = useCallback((trade) => {
    setSelectedId(trade.id);
  }, []);

  return (
    <section>
      <h2>Trades</h2>

      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(event) =>
          setSearch(event.target.value.toUpperCase())
        }
      />

      {selectedId !== null && (
        <p>Selected trade: {selectedId}</p>
      )}

      <DataTable data={data.items} pageSize={20}>
        <DataTable.Header
          columns={[
            { key: 'tradeRef', label: 'Ref' },
            { key: 'symbol', label: 'Symbol' },
            { key: 'qty', label: 'Qty' },
            { key: 'price', label: 'Price' },
            { key: 'status', label: 'Status' },
          ]}
        />

        <DataTable.Body
          renderRow={(trade) => (
            <TradeRow
              key={trade.id ?? trade.tradeRef}
              trade={trade}
              onClick={handleSelect}
            />
          )}
        />

        <DataTable.Pagination />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);
