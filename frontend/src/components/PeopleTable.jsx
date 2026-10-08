export default function PeopleTable({ people, loading, selectedId, onRefresh, onSelect, onDelete }) {
  return (
    <section className="card">
      <div className="card-header">
        <h2>Pessoas cadastradas</h2>
        <button className="secondary" onClick={onRefresh} disabled={loading}>
          {loading ? 'Carregando…' : 'Atualizar'}
        </button>
      </div>

      {people.length === 0 ? (
        <p className="muted">{loading ? 'Carregando…' : 'Nenhuma pessoa cadastrada.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Documento</th>
              <th>Nome</th>
              <th>Sobrenome</th>
              <th>E-mail</th>
              <th aria-label="Ações" />
            </tr>
          </thead>
          <tbody>
            {people.map((person) => (
              <tr key={person.id} className={person.id === selectedId ? 'selected' : undefined}>
                <td>{person.id}</td>
                <td>{person.document}</td>
                <td>{person.name}</td>
                <td>{person.surname}</td>
                <td>{person.email}</td>
                <td className="actions">
                  <button onClick={() => onSelect(person)}>Nacionalidade</button>
                  <button className="danger" onClick={() => onDelete(person)}>
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
