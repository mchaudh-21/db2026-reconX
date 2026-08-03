import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import DataTable from '../DataTable.jsx';

const columns = [
  { key: 'name', label: 'Name' },
  { key: 'value', label: 'Value' },
];

const rows = [
  { id: 1, name: 'Beta', value: 2 },
  { id: 2, name: 'Alpha', value: 1 },
];

function renderTable(data = rows) {
  return render(
    <DataTable data={data}>
      <DataTable.Header columns={columns} />

      <DataTable.Body
        renderRow={(row) => (
          <div role="row" key={row.id}>
            <span role="cell">{row.name}</span>
            <span role="cell">{row.value}</span>
          </div>
        )}
      />

      <DataTable.Pagination />
    </DataTable>,
  );
}

describe('<DataTable>', () => {
  it('renders columns and rows', () => {
    renderTable();

    expect(
      screen.getByRole('button', { name: 'Name' }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'Value' }),
    ).toBeInTheDocument();

    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.getByText('Beta')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('sorts rows when a header is clicked', async () => {
    const user = userEvent.setup();

    renderTable();

    await user.click(
      screen.getByRole('button', { name: 'Name' }),
    );

    const body = document.querySelector('.data-table__body');
    const renderedRows = within(body).getAllByRole('row');

    expect(
      within(renderedRows[0]).getByText('Alpha'),
    ).toBeInTheDocument();

    expect(
      within(renderedRows[1]).getByText('Beta'),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole('button', { name: 'Name' }),
    );

    const descendingRows = within(body).getAllByRole('row');

    expect(
      within(descendingRows[0]).getByText('Beta'),
    ).toBeInTheDocument();

    expect(
      within(descendingRows[1]).getByText('Alpha'),
    ).toBeInTheDocument();
  });
});
