import { CircleFeed } from '../components/CircleFeed';

export function AllDiscussionsPage() {
  return (
    <CircleFeed
      scope="ALL"
      emptyTitle="No circles yet"
      emptyBody="Be the first to start one — use the Create button up top."
    />
  );
}
