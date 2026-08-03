import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <div
      className="data-table__row"
      role="row"
      onClick={() => onClick?.(trade)}
    >
      <span role="cell">{trade.tradeRef}</span>
      <span role="cell">{trade.symbol}</span>
      <span role="cell">{trade.qty}</span>
      <span role="cell">{trade.price}</span>
      <span role="cell">{trade.status}</span>
    </div>
  );
}

function areEqual(previousProps, nextProps) {
  return (
    previousProps.trade.id === nextProps.trade.id &&
    previousProps.trade.status === nextProps.trade.status &&
    previousProps.trade.price === nextProps.trade.price &&
    previousProps.onClick === nextProps.onClick
  );
}

const TradeRow = React.memo(TradeRowImpl, areEqual);

TradeRow.displayName = 'TradeRow';

export default TradeRow;
