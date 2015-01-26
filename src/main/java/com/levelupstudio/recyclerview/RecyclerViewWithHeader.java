package com.levelupstudio.recyclerview;

import java.util.ArrayList;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A {@code RecyclerView} class with dynamic support for adding header/footers, with the same API as a {@link android.widget.ListView ListView}.
 *
 * @author Created by robUx4 on 06/10/2014.
 */
public class RecyclerViewWithHeader extends RecyclerView {
	public RecyclerViewWithHeader(Context context) {
		super(context);
	}

	public RecyclerViewWithHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RecyclerViewWithHeader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Add a fixed view to appear at the top of the list. If addHeaderView is
	 * called more than once, the views will appear in the order they were
	 * added. Views added using this call can take focus if they want.
	 *
	 * @param v The view to add.
	 */
	public void addHeaderView(View v) {
		AdapterWithHeader adapter = (AdapterWithHeader) super.getAdapter();
		if (null != adapter) {
			adapter.addHeaderView(v);
		}
	}

	/**
	 * Add a fixed view to appear at the bottom of the list. If addFooterView is
	 * called more than once, the views will appear in the order they were
	 * added. Views added using this call can take focus if they want.
	 *
	 * @param v The view to add.
	 */
	public void addFooterView(View v) {
		AdapterWithHeader adapter = (AdapterWithHeader) super.getAdapter();
		if (null != adapter) {
			adapter.addFooterView(v);
		}
	}

	/**
	 * Removes a previously-added header view.
	 *
	 * @param v The view to remove
	 * @return true if the view was removed, false if the view was not a header
	 * view
	 */
	public void removeHeaderView(View v) {
		AdapterWithHeader adapter = (AdapterWithHeader) super.getAdapter();
		if (null != adapter) {
			adapter.removeHeaderView(v);
		}
	}

	/**
	 * Removes a previously-added footer view.
	 *
	 * @param v The view to remove
	 * @return true if the view was removed, false if the view was not a footer view
	 */
	public void removeFooterView(View v) {
		AdapterWithHeader adapter = (AdapterWithHeader) super.getAdapter();
		if (null != adapter) {
			adapter.removeFooterView(v);
		}
	}

	@Override
	public void setAdapter(Adapter adapter) {
		if (null!=adapter) {
			adapter = new AdapterWithHeader(adapter);
		}
		super.setAdapter(adapter);
	}

	@Override
	public Adapter getAdapter() {
		Adapter result = super.getAdapter();
		if (result instanceof AdapterWithHeader) {
			result = ((AdapterWithHeader) result).delegate;
		}
		return result;
	}

	/**
	 * Returns the number of header views in the list. Header views are special views
	 * at the top of the list that should not be recycled during a layout.
	 *
	 * @return The number of header views, 0 in the default implementation.
	 */
	public int getHeaderViewsCount() {
		Adapter result = super.getAdapter();
		if (result instanceof AdapterWithHeader) {
			return ((AdapterWithHeader) result).getHeaderViewsCount();
		}
		return 0;
	}

	/**
	 * Returns the number of footer views in the list. Footer views are special views
	 * at the bottom of the list that should not be recycled during a layout.
	 *
	 * @return The number of footer views, 0 in the default implementation.
	 */
	public int getFooterViewsCount() {
		Adapter result = super.getAdapter();
		if (result instanceof AdapterWithHeader) {
			return ((AdapterWithHeader) result).getFooterViewsCount();
		}
		return 0;
	}

	@Override
	public int computeVerticalScrollExtent() {
		return super.computeVerticalScrollExtent();
	}

	@Override
	public int computeVerticalScrollOffset() {
		return super.computeVerticalScrollOffset();
	}

	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

	private static class AdapterWithHeader<VH extends ViewHolder> extends Adapter<ViewHolder> {
		final Adapter<VH> delegate;
		private final ArrayList<HeaderViewHolder> headerViews = new ArrayList<>(1);
		private final ArrayList<HeaderViewHolder> footerViews = new ArrayList<>(1);
		private static final int VIEW_TYPE_OFFSET = 7000;

		private static class HeaderViewHolder extends ViewHolder {
			public HeaderViewHolder(View itemView) {
				super(itemView);
			}
		}

		public AdapterWithHeader(@NonNull Adapter<VH> adapter) {
			this.delegate = adapter;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType < VIEW_TYPE_OFFSET || viewType >= (VIEW_TYPE_OFFSET + headerViews.size() + footerViews.size()))
				return delegate.onCreateViewHolder(parent, viewType);
			viewType -= VIEW_TYPE_OFFSET;
			if (viewType < headerViews.size())
				return headerViews.get(viewType);
			viewType -= headerViews.size();
			return footerViews.get(viewType);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (position >= headerViews.size()) {
				position -= headerViews.size();
				if (position < delegate.getItemCount()) {
					delegate.onBindViewHolder((VH) holder, position);
				}
			}
		}

		@Override
		public final int getItemCount() {
			return headerViews.size() + footerViews.size() + delegate.getItemCount();
		}

		@Override
		public int getItemViewType(int position) {
			if (position < headerViews.size())
				return position + VIEW_TYPE_OFFSET;
			position -= headerViews.size();
			if (position < delegate.getItemCount()) {
				return delegate.getItemViewType(position);
			}
			position -= delegate.getItemCount();
			return position + VIEW_TYPE_OFFSET + headerViews.size();
		}

		@Override
		public long getItemId(int position) {
			if (position < headerViews.size())
				return position + VIEW_TYPE_OFFSET;
			position -= headerViews.size();
			if (position < delegate.getItemCount()) {
				return delegate.getItemId(position);
			}
			return position + VIEW_TYPE_OFFSET + headerViews.size();
		}

		@Override
		public void setHasStableIds(boolean hasStableIds) {
			delegate.setHasStableIds(hasStableIds);
		}

		@Override
		public void onViewRecycled(ViewHolder holder) {
			if (!(holder instanceof HeaderViewHolder))
				delegate.onViewRecycled((VH) holder);
		}

		@Override
		public void onViewAttachedToWindow(ViewHolder holder) {
			if (!(holder instanceof HeaderViewHolder))
				delegate.onViewAttachedToWindow((VH) holder);
		}

		@Override
		public void onViewDetachedFromWindow(ViewHolder holder) {
			if (!(holder instanceof HeaderViewHolder))
				delegate.onViewDetachedFromWindow((VH) holder);
		}

		@Override
		public void registerAdapterDataObserver(AdapterDataObserver observer) {
			delegate.registerAdapterDataObserver(observer);
		}

		@Override
		public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
			delegate.unregisterAdapterDataObserver(observer);
		}

		public void addHeaderView(View headerView) {
			headerViews.add(new HeaderViewHolder(headerView));
			notifyDataSetChanged();
		}

		public void addFooterView(View headerView) {
			footerViews.add(new HeaderViewHolder(headerView));
			notifyDataSetChanged();
		}

		public void removeHeaderView(View headerView) {
			for (HeaderViewHolder headers : headerViews) {
				if (headers.itemView == headerView) {
					headerViews.remove(headers);
					notifyDataSetChanged();
					break;
				}
			}
		}

		public void removeFooterView(View headerView) {
			for (HeaderViewHolder headers : headerViews) {
				if (headers.itemView == headerView) {
					footerViews.remove(headers);
					notifyDataSetChanged();
					break;
				}
			}
		}

		public int getHeaderViewsCount() {
			return headerViews.size();
		}

		public int getFooterViewsCount() {
			return footerViews.size();
		}
	}
}
