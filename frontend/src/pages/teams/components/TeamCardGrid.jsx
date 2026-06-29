import TeamCard from './TeamCard'

export default function TeamCardGrid({
  teams,
  onAssignLeader,
  onAddMember,
}) {
  return (
    <div className="team-card-grid">
      {teams.map((team) => (
        <TeamCard
          key={team.id}
          team={team}
          onAssignLeader={onAssignLeader}
          onAddMember={onAddMember}
        />
      ))}
    </div>
  )
}
