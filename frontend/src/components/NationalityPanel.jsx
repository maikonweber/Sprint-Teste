import { useEffect, useState } from 'react';

const percent = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 1 });

export default function NationalityPanel({ person, findNationality }) {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!person) {
      setResult(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setResult(null);
    setError(null);
    findNationality(person.id)
      .then((data) => !cancelled && setResult(data))
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
    // findNationality é recriada a cada render do App; a consulta só deve ser refeita quando a pessoa muda.
  }, [person?.id]);

  return (
    <section className="card">
      <h2>Nacionalidade provável</h2>
      {!person && <p className="muted">Selecione uma pessoa na tabela para consultar a nacionalidade.</p>}
      {loading && <p className="muted">Consultando Nationalize.io…</p>}
      {error && <p className="error">{error}</p>}
      {result && (
        <dl className="details">
          <dt>Nome</dt>
          <dd>{result.name}</dd>
          <dt>Nacionalidade provável</dt>
          <dd>{result.nationality}</dd>
          <dt>Código ISO</dt>
          <dd>{result.countryCode ?? '—'}</dd>
          <dt>Probabilidade</dt>
          <dd>{result.probability != null ? percent.format(result.probability) : '—'}</dd>
        </dl>
      )}
    </section>
  );
}
