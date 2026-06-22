import { gql } from '@apollo/client';

/**
 * GraphQL operations for the circles flow.
 *
 * The `CircleFields` fragment is reused everywhere so the Apollo cache stays
 * normalised — joining/leaving a circle in one feed updates it in all the
 * others that have the same Circle in their cache.
 */
export const CIRCLE_FIELDS = gql`
  fragment CircleFields on Circle {
    id
    topic
    description
    memberCount
    adminUserId
    isAdmin
    isMember
    createdAt
  }
`;

export const CIRCLES_QUERY = gql`
  query Circles(
    $scope: CircleScope!
    $sort: CircleSort
    $search: String
    $page: Int
    $limit: Int
  ) {
    circles(scope: $scope, sort: $sort, search: $search, page: $page, limit: $limit) {
      total
      page
      limit
      data {
        ...CircleFields
      }
    }
  }
  ${CIRCLE_FIELDS}
`;

export const CIRCLE_QUERY = gql`
  query Circle($id: UUID!) {
    circle(id: $id) {
      ...CircleFields
    }
  }
  ${CIRCLE_FIELDS}
`;

export const CREATE_CIRCLE_MUTATION = gql`
  mutation CreateCircle($topic: String!, $description: String!) {
    createCircle(topic: $topic, description: $description) {
      ...CircleFields
    }
  }
  ${CIRCLE_FIELDS}
`;

export const UPDATE_CIRCLE_DESCRIPTION_MUTATION = gql`
  mutation UpdateCircleDescription($id: UUID!, $description: String!) {
    updateCircleDescription(id: $id, description: $description) {
      ...CircleFields
    }
  }
  ${CIRCLE_FIELDS}
`;

export const DELETE_CIRCLE_MUTATION = gql`
  mutation DeleteCircle($id: UUID!) {
    deleteCircle(id: $id) {
      success
    }
  }
`;

export const JOIN_CIRCLE_MUTATION = gql`
  mutation JoinCircle($id: UUID!) {
    joinCircle(id: $id) {
      ...CircleFields
    }
  }
  ${CIRCLE_FIELDS}
`;

export const LEAVE_CIRCLE_MUTATION = gql`
  mutation LeaveCircle($id: UUID!) {
    leaveCircle(id: $id) {
      ...CircleFields
    }
  }
  ${CIRCLE_FIELDS}
`;
