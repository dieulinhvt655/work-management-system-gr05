import TeamCard from './TeamCard'

export default function TeamCardGrid({
  teams,
  onAddMember,
}) {
  return (
    <div className="team-card-grid">
      {teams.map((team) => (
        <TeamCard
          key={team.id}
          team={team}
          onAddMember={onAddMember}
        />
      ))}
    </div>
  )
}
